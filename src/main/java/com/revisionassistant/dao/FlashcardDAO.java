package com.revisionassistant.dao;
import com.revisionassistant.database.DatabaseManager;
import com.revisionassistant.session.CurrentUser;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.revisionassistant.model.Flashcard;
import com.revisionassistant.model.RevisionStatus;
import java.sql.Types;

public class FlashcardDAO {
    private static final String COLUMNS="id,subject_id,topic_id,front,back,difficult,revision_status";
    public Flashcard insert(Flashcard x)throws SQLException{String sql="INSERT INTO flashcards(user_id,subject_id,topic_id,front,back,difficult,revision_status) VALUES(?,?,?,?,?,?,?)";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){s.setInt(1,CurrentUser.get().getId());bind(s,x,2);s.executeUpdate();try(ResultSet k=s.getGeneratedKeys()){if(k.next())x.setId(k.getInt(1));}}return x;}
    public List<Flashcard> findAll()throws SQLException{String sql="SELECT "+COLUMNS+" FROM flashcards WHERE user_id=? ORDER BY subject_id,id";List<Flashcard> out=new ArrayList<>();try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,CurrentUser.get().getId());try(ResultSet r=s.executeQuery()){while(r.next())out.add(mapRow(r));}}return out;}
    public void update(Flashcard x)throws SQLException{String sql="UPDATE flashcards SET subject_id=?,topic_id=?,front=?,back=?,difficult=?,revision_status=? WHERE id=? AND user_id=?";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){bind(s,x,1);s.setInt(7,x.getId());s.setInt(8,CurrentUser.get().getId());s.executeUpdate();}}
    public void updateDifficult(int id,boolean v)throws SQLException{String sql="UPDATE flashcards SET difficult=? WHERE id=? AND user_id=?";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,v?1:0);s.setInt(2,id);s.setInt(3,CurrentUser.get().getId());s.executeUpdate();}}
    public void updateRevisionStatus(int id,RevisionStatus v)throws SQLException{String sql="UPDATE flashcards SET revision_status=? WHERE id=? AND user_id=?";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setString(1,v.name());s.setInt(2,id);s.setInt(3,CurrentUser.get().getId());s.executeUpdate();}}
    public void delete(int id)throws SQLException{String sql="DELETE FROM flashcards WHERE id=? AND user_id=?";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,id);s.setInt(2,CurrentUser.get().getId());s.executeUpdate();}}
    public int countBySubjectId(int id)throws SQLException{return count("subject_id=?",id);} public int countByTopicId(int id)throws SQLException{return count("topic_id=?",id);}
    private int count(String clause,int id)throws SQLException{String sql="SELECT COUNT(*) FROM flashcards WHERE "+clause+" AND user_id=?";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,id);s.setInt(2,CurrentUser.get().getId());try(ResultSet r=s.executeQuery()){if(r.next())return r.getInt(1);}}return 0;}
    private void bind(PreparedStatement s,Flashcard x,int o)throws SQLException{s.setInt(o,x.getSubjectId());if(x.getTopicId()==null)s.setNull(o+1,Types.INTEGER);else s.setInt(o+1,x.getTopicId());s.setString(o+2,x.getFront());s.setString(o+3,x.getBack());s.setInt(o+4,x.isDifficult()?1:0);s.setString(o+5,x.getRevisionStatus().name());}
    private Flashcard mapRow(ResultSet r)throws SQLException{int tv=r.getInt("topic_id");Integer topic=r.wasNull()?null:tv;return new Flashcard(r.getInt("id"),r.getInt("subject_id"),topic,r.getString("front"),r.getString("back"),r.getInt("difficult")==1,RevisionStatus.fromString(r.getString("revision_status")));}
}
