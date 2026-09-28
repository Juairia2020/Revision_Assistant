package com.revisionassistant.dao;
import com.revisionassistant.database.DatabaseManager;
import com.revisionassistant.session.CurrentUser;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.revisionassistant.model.Topic;

public class TopicDAO {
    public Topic insert(Topic topic)throws SQLException{String sql="INSERT INTO topics(user_id,subject_id,name,completed) VALUES(?,?,?,?)";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){s.setInt(1,CurrentUser.get().getId());s.setInt(2,topic.getSubjectId());s.setString(3,topic.getName());s.setInt(4,topic.isCompleted()?1:0);s.executeUpdate();try(ResultSet k=s.getGeneratedKeys()){if(k.next())topic.setId(k.getInt(1));}}return topic;}
    public List<Topic> findBySubjectId(int subjectId)throws SQLException{return query("SELECT id,subject_id,name,completed FROM topics WHERE subject_id=? AND user_id=? ORDER BY name",subjectId);}
    public Topic findById(int id)throws SQLException{String sql="SELECT id,subject_id,name,completed FROM topics WHERE id=? AND user_id=?";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,id);s.setInt(2,CurrentUser.get().getId());try(ResultSet r=s.executeQuery()){if(r.next())return mapRow(r);}}return null;}
    public List<Topic> findAll()throws SQLException{String sql="SELECT id,subject_id,name,completed FROM topics WHERE user_id=? ORDER BY subject_id,name";List<Topic> out=new ArrayList<>();try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,CurrentUser.get().getId());try(ResultSet r=s.executeQuery()){while(r.next())out.add(mapRow(r));}}return out;}
    public int countBySubjectId(int subjectId)throws SQLException{return count("subject_id=?",subjectId);}
    public void update(Topic topic)throws SQLException{String sql="UPDATE topics SET subject_id=?,name=?,completed=? WHERE id=? AND user_id=?";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,topic.getSubjectId());s.setString(2,topic.getName());s.setInt(3,topic.isCompleted()?1:0);s.setInt(4,topic.getId());s.setInt(5,CurrentUser.get().getId());s.executeUpdate();}}
    public void updateCompleted(int id,boolean completed)throws SQLException{String sql="UPDATE topics SET completed=? WHERE id=? AND user_id=?";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,completed?1:0);s.setInt(2,id);s.setInt(3,CurrentUser.get().getId());s.executeUpdate();}}
    public void delete(int id)throws SQLException{String sql="DELETE FROM topics WHERE id=? AND user_id=?";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,id);s.setInt(2,CurrentUser.get().getId());s.executeUpdate();}}
    private List<Topic> query(String sql,int subjectId)throws SQLException{List<Topic> out=new ArrayList<>();try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,subjectId);s.setInt(2,CurrentUser.get().getId());try(ResultSet r=s.executeQuery()){while(r.next())out.add(mapRow(r));}}return out;}
    private int count(String clause,int id)throws SQLException{String sql="SELECT COUNT(*) FROM topics WHERE "+clause+" AND user_id=?";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,id);s.setInt(2,CurrentUser.get().getId());try(ResultSet r=s.executeQuery()){if(r.next())return r.getInt(1);}}return 0;}
    private Topic mapRow(ResultSet r)throws SQLException{return new Topic(r.getInt("id"),r.getInt("subject_id"),r.getString("name"),r.getInt("completed")==1);}
}
