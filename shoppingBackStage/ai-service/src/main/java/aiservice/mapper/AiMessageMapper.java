package aiservice.mapper;

import aiservice.entity.AiMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

/** AI 消息表访问 */
@Mapper
public interface AiMessageMapper {

    @Insert("INSERT INTO ai_message(session_id,role,msg_type,content) " +
            "VALUES(#{sessionId},#{role},#{msgType},#{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiMessage message);

    /** 组装多轮上下文用：只取文本消息，图片历史不传（控制 token 与请求体大小） */
    @Select("SELECT * FROM ai_message WHERE session_id=#{sessionId} AND msg_type='text' ORDER BY id ASC")
    List<AiMessage> listTextBySession(String sessionId);

    /** 前端展示用：含图片消息，按时间正序 */
    @Select("SELECT * FROM ai_message WHERE session_id=#{sessionId} ORDER BY id ASC")
    List<AiMessage> listAllBySession(String sessionId);

    @Delete("DELETE FROM ai_message WHERE session_id=#{sessionId}")
    int deleteBySession(String sessionId);
}
