package aiservice.controller;

import aiservice.entity.Knowledge;
import aiservice.mapper.KnowledgeMapper;
import aiservice.service.KnowledgeService;
import model.Result;
import model.ResultCodeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 知识库管理（仅管理员 role=1，接口不在网关白名单）。
 *  GET    /ai/kb          知识列表（正文只返回前 80 字）
 *  POST   /ai/kb          新增知识（标题+正文），自动切块向量化
 *  DELETE /ai/kb/{id}     删除知识并重建索引
 *  POST   /ai/kb/rebuild  手动全量重建内存索引
 */
@RestController
@RequestMapping("/ai/kb")
public class KnowledgeController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeController.class);

    @Autowired
    private KnowledgeMapper knowledgeMapper;
    @Autowired
    private KnowledgeService knowledgeService;

    /** 知识列表 */
    @GetMapping
    public Result<List<Knowledge>> list(@RequestHeader(value = "role", defaultValue = "0") String role) {
        if (!"1".equals(role)) {
            return Result.failure(ResultCodeEnum.FAIL, "无管理员权限");
        }
        return Result.success(knowledgeMapper.listBrief());
    }

    /** 新增知识：正文切块后调用向量化模型入内存索引 */
    @PostMapping
    public Result<String> create(@RequestHeader(value = "role", defaultValue = "0") String role,
                                 @RequestBody Knowledge knowledge) {
        if (!"1".equals(role)) {
            return Result.failure(ResultCodeEnum.FAIL, "无管理员权限");
        }
        if (!StringUtils.hasText(knowledge.getTitle()) || !StringUtils.hasText(knowledge.getContent())) {
            return Result.failure(ResultCodeEnum.FAIL, "标题和正文不能为空");
        }
        try {
            knowledgeService.addKnowledge(knowledge);
            return Result.success("已入库并完成向量化");
        } catch (Exception e) {
            // 原文已入库但向量化失败：提示检查 embedding 模型，可在开通后点重建
            log.warn("知识向量化失败：{}", e.getMessage());
            return Result.failure(ResultCodeEnum.FAIL,
                    "已保存原文，但向量化失败（请确认方舟已开通文本向量化模型），开通后请点“重建索引”");
        }
    }

    /** 删除知识 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestHeader(value = "role", defaultValue = "0") String role,
                               @PathVariable Long id) {
        if (!"1".equals(role)) {
            return Result.failure(ResultCodeEnum.FAIL, "无管理员权限");
        }
        knowledgeService.deleteKnowledge(id);
        return Result.success();
    }

    /** 全量重建索引（embedding 模型开通后使用） */
    @PostMapping("/rebuild")
    public Result<Map<String, Object>> rebuild(@RequestHeader(value = "role", defaultValue = "0") String role) {
        if (!"1".equals(role)) {
            return Result.failure(ResultCodeEnum.FAIL, "无管理员权限");
        }
        try {
            knowledgeService.rebuild();
            return Result.success(Map.of("ready", knowledgeService.isReady()));
        } catch (Exception e) {
            return Result.failure(ResultCodeEnum.FAIL, "重建失败：" + e.getMessage());
        }
    }
}
