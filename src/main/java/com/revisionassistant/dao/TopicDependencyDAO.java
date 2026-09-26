package com.revisionassistant.dao;

import com.revisionassistant.database.DatabaseManager;
import com.revisionassistant.model.TopicDependency;
import com.revisionassistant.session.CurrentUser;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TopicDependencyDAO {
    public TopicDependency insert(int topicId,int prerequisiteId)throws SQLException{
        String sql="INSERT INTO topic_dependencies(user_id,topic_id,prerequisite_id) VALUES(?,?,?)";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){
            s.setInt(1,CurrentUser.get().getId());s.setInt(2,topicId);s.setInt(3,prerequisiteId);s.executeUpdate();TopicDependency d=new TopicDependency(topicId,prerequisiteId);try(ResultSet k=s.getGeneratedKeys()){if(k.next())d.setId(k.getInt(1));}return d;}
    }
    public List<TopicDependency> findAll()throws SQLException{String sql="SELECT id,topic_id,prerequisite_id FROM topic_dependencies WHERE user_id=?";List<TopicDependency> out=new ArrayList<>();try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,CurrentUser.get().getId());try(ResultSet r=s.executeQuery()){while(r.next())out.add(mapRow(r));}}return out;}
    public List<Integer> findPrerequisitesOf(int id)throws SQLException{return queryIds("SELECT prerequisite_id FROM topic_dependencies WHERE topic_id=? AND user_id=?",id);}
    public List<Integer> findDependentsOf(int id)throws SQLException{return queryIds("SELECT topic_id FROM topic_dependencies WHERE prerequisite_id=? AND user_id=?",id);}
    public void delete(int topicId,int prerequisiteId)throws SQLException{String sql="DELETE FROM topic_dependencies WHERE topic_id=? AND prerequisite_id=? AND user_id=?";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,topicId);s.setInt(2,prerequisiteId);s.setInt(3,CurrentUser.get().getId());s.executeUpdate();}}
    public void deleteAllForTopic(int id)throws SQLException{String sql="DELETE FROM topic_dependencies WHERE (topic_id=? OR prerequisite_id=?) AND user_id=?";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,id);s.setInt(2,id);s.setInt(3,CurrentUser.get().getId());s.executeUpdate();}}
    private List<Integer> queryIds(String sql,int id)throws SQLException{List<Integer> out=new ArrayList<>();try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,id);s.setInt(2,CurrentUser.get().getId());try(ResultSet r=s.executeQuery()){while(r.next())out.add(r.getInt(1));}}return out;}
    private TopicDependency mapRow(ResultSet r)throws SQLException{return new TopicDependency(r.getInt("id"),r.getInt("topic_id"),r.getInt("prerequisite_id"));}
}
