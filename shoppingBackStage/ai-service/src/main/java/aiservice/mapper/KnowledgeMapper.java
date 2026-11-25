package aiservice.mapper;

import aiservice.entity.Knowledge;
import org.apache.ibatis.annotations.*;

import java.util.List;

/** 知识库文档表访问 */
@Mapper
public interface KnowledgeMapper {

    @Insert("INSERT INTO ai_knowledge(title,content) VALUES(#{title},#{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Knowledge knowledge);

    @Select("SELECT id,title,LEFT(content,80) AS content,create_time FROM ai_knowledge ORDER BY id DESC")
    List<Knowledge> listBrief();

    @Select("SELECT * FROM ai_knowledge ORDER BY id ASC")
    List<Knowledge> listAll();

    @Delete("DELETE FROM ai_knowledge WHERE id=#{id}")
    int deleteById(Long id);
}
