package adminservice.mapper;

import model.ChatMessage;
import model.ChatSession;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 人工客服会话/消息表访问。
 * chat_session.status：0=待接入 1=进行中 2=已关闭
 * chat_message.sender_type：0=用户 1=客服
 */
@Mapper
public interface ChatMapper {

    /** 用户查自己的进行中/待接入会话，没有则插入一个待接入会话 */
    @Select("SELECT * FROM chat_session WHERE user_id=#{userId} AND status IN (0,1) ORDER BY id DESC LIMIT 1")
    ChatSession findActiveSession(Long userId);

    /** 根据 ID 查会话 */
    @Select("SELECT * FROM chat_session WHERE id=#{sessionId}")
    ChatSession findById(Long sessionId);

    @Insert("INSERT INTO chat_session(user_id,status) VALUES(#{userId},0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertPendingSession(ChatSession session);

    /** 客服端：查所有待接入会话（带用户名） */
    @Select("SELECT s.*, u.username FROM chat_session s LEFT JOIN user u ON s.user_id=u.id " +
            "WHERE s.status=0 ORDER BY s.create_time ASC")
    List<ChatSession> listPendingSessions();

    /** 客服端：查自己已接入且未关闭的会话 */
    @Select("SELECT s.*, u.username FROM chat_session s LEFT JOIN user u ON s.user_id=u.id " +
            "WHERE s.cs_id=#{csId} AND s.status=1 ORDER BY s.update_time DESC")
    List<ChatSession> listCsActiveSessions(Long csId);

    /** 客服接入会话（并发防护：仅当 cs_id 为 NULL 时才能接入） */
    @Update("UPDATE chat_session SET cs_id=#{csId}, status=1 WHERE id=#{sessionId} AND cs_id IS NULL")
    int bindCs(@Param("sessionId") Long sessionId, @Param("csId") Long csId);

    /** 关闭会话 */
    @Update("UPDATE chat_session SET status=2 WHERE id=#{sessionId}")
    int closeSession(Long sessionId);

    /** 查会话历史消息 */
    @Select("SELECT * FROM chat_message WHERE session_id=#{sessionId} ORDER BY id ASC")
    List<ChatMessage> listMessages(Long sessionId);

    /** 插入消息 */
    @Insert("INSERT INTO chat_message(session_id,sender_id,sender_type,content) " +
            "VALUES(#{sessionId},#{senderId},#{senderType},#{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMessage(ChatMessage message);

    /** 增加未读计数（用户留言） */
    @Update("UPDATE chat_session SET unread_count=unread_count+1 WHERE id=#{sessionId}")
    int incrUnread(Long sessionId);

    /** 清零未读（客服已读） */
    @Update("UPDATE chat_session SET unread_count=0 WHERE id=#{sessionId}")
    int resetUnread(Long sessionId);

    /** 标记某会话中某方向的消息为已读 */
    @Update("UPDATE chat_message SET is_read=1 WHERE session_id=#{sessionId} AND sender_type=#{senderType}")
    int markRead(@Param("sessionId") Long sessionId, @Param("senderType") int senderType);
}
