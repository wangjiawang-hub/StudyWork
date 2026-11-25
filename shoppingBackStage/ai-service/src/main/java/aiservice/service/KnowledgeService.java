package aiservice.service;

import aiservice.entity.Knowledge;
import aiservice.mapper.KnowledgeMapper;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RAG 知识库服务。
 * 做法（零额外中间件）：
 *  1. 知识原文存 MySQL（ai_knowledge）；
 *  2. 启动时把全部文档按固定长度切块、调用向量化模型，构建内存向量索引；
 *  3. 提问时把问题向量化，余弦相似度取 TopK 片段，拼进 system prompt。
 * 向量化模型未开通/调用失败时 ready=false，问答自动降级为普通大模型回答，不影响主流程。
 */
@Service
public class  KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    /** 每块约 400 字符，相邻块重叠 60 字符，避免语义被切断 */
    private static final int CHUNK_SIZE = 400;
    private static final int CHUNK_OVERLAP = 60;
    /** 每次检索最多取 3 段 */
    private static final int TOP_K = 3;
    /** 相似度门槛，低于此值不引用 */
    private static final double MIN_SCORE = 0.5;

    @Autowired
    private KnowledgeMapper knowledgeMapper;
    @Autowired
    private EmbeddingModel embeddingModel;

    private final DocumentSplitter splitter = DocumentSplitters.recursive(CHUNK_SIZE, CHUNK_OVERLAP);

    /** 内存向量索引，重建时整体替换引用（volatile 保证可见性） */
    private volatile InMemoryEmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();
    /** 索引是否可用（向量化模型不可用时为 false） */
    private volatile boolean ready = false;

    /** 服务启动：全量重建内存索引，失败只告警不阻断启动 */
    @PostConstruct
    public void init() {
        try {
            rebuild();
        } catch (Exception e) {
            log.warn("知识库索引初始化失败，RAG 将降级关闭：{}", e.getMessage());
        }
    }

    public boolean isReady() {
        return ready;
    }

    /**
     * 新增知识：先入库，再切块向量化追加进内存索引。
     * 向量化失败会抛出异常，由接口层提示开通 embedding 模型。
     */
    public synchronized void addKnowledge(Knowledge knowledge) {
        knowledgeMapper.insert(knowledge);
        indexOne(knowledge);
    }

    /** 删除知识：删库后整体重建（文档量小，简单可靠） */
    public synchronized void deleteKnowledge(Long id) {
        knowledgeMapper.deleteById(id);
        rebuild();
    }

    /** 全量重建内存索引 */
    public synchronized void rebuild() {
        List<Knowledge> all = knowledgeMapper.listAll();
        InMemoryEmbeddingStore<TextSegment> fresh = new InMemoryEmbeddingStore<>();
        for (Knowledge k : all) {
            Document doc = Document.from(k.getContent(),
                    new dev.langchain4j.data.document.Metadata()
                            .put("kid", String.valueOf(k.getId()))
                            .put("title", k.getTitle() == null ? "" : k.getTitle()));
            List<TextSegment> segments = splitter.split(doc);
            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
            for (int i = 0; i < segments.size(); i++) {
                fresh.add(embeddings.get(i), segments.get(i));
            }
        }
        this.store = fresh;
        this.ready = true;
        log.info("知识库索引构建完成，文档 {} 篇", all.size());
    }

    /**
     * 检索与问题最相关的知识片段，拼成可直接放入 prompt 的文本；
     * 索引不可用或无命中时返回空串（调用方据此走普通回答）。
     */
    public String retrieveContext(String question) {
        if (!ready || question == null || question.isBlank()) {
            return "";
        }
        try {
            Embedding queryEmbedding = embeddingModel.embed(question).content();
            List<EmbeddingMatch<TextSegment>> matches = store.search(
                            EmbeddingSearchRequest.builder()
                                    .queryEmbedding(queryEmbedding)
                                    .maxResults(TOP_K)
                                    .minScore(MIN_SCORE)
                                    .build())
                    .matches();
            if (matches.isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (EmbeddingMatch<TextSegment> m : matches) {
                String title = m.embedded().metadata().getString("title");
                sb.append("【").append(title).append("】").append(m.embedded().text()).append("\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("知识库检索失败，本次按普通问答处理：{}", e.getMessage());
            return "";
        }
    }

    /** 单篇文档切块入索引 */
    private void indexOne(Knowledge k) {
        Document doc = Document.from(k.getContent(),
                new dev.langchain4j.data.document.Metadata()
                        .put("kid", String.valueOf(k.getId()))
                        .put("title", k.getTitle() == null ? "" : k.getTitle()));
        List<TextSegment> segments = splitter.split(doc);
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        for (int i = 0; i < segments.size(); i++) {
            store.add(embeddings.get(i), segments.get(i));
        }
        ready = true;
    }
}
