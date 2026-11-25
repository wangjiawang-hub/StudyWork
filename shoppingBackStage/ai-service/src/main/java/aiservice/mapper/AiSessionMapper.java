package aiservice.mapper;

import aiservice.entity.AiSession;
import org.apache.ibatis.annotations.*;

import java.util.List;

/** AI 会话表访问 */
@Mapper
public interface AiSessionMapper {

    @Insert("INSERT INTO ai_session(id,user_id,title) VALUES(#{id},#{userId},#{title})")
    int insert(AiSession session);

    @Select("SELECT * FROM ai_session WHERE id=#{id}")
    AiSession findById(String id);

    /** 登录用户的会话列表，按最近发言倒序 */
    @Select("SELECT * FROM ai_session WHERE user_id=#{userId} ORDER BY update_time DESC")
    List<AiSession> listByUser(Long userId);

    /** 首条提问时用问题前 20 字作为会话标题 */
    @Update("UPDATE ai_session SET title=#{title} WHERE id=#{id} AND title='新会话'")
    int updateTitle(@Param("id") String id, @Param("title") String title);

    @Delete("DELETE FROM ai_session WHERE id=#{id}")
    int deleteById(String id);
}
