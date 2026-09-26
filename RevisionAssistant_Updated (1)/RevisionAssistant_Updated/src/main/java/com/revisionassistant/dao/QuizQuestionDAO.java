package com.revisionassistant.dao;
import com.revisionassistant.database.DatabaseManager;
import com.revisionassistant.session.CurrentUser;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.revisionassistant.model.QuizOption;
import com.revisionassistant.model.QuizQuestion;
import java.sql.Types;

public class QuizQuestionDAO {
    private static final String COLUMNS="id,subject_id,topic_id,question_text,option_a,option_b,option_c,option_d,correct_option";
    public QuizQuestion insert(QuizQuestion x)throws SQLException{String sql="INSERT INTO quiz_questions(user_id,subject_id,topic_id,question_text,option_a,option_b,option_c,option_d,correct_option) VALUES(?,?,?,?,?,?,?,?,?)";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){s.setInt(1,CurrentUser.get().getId());bind(s,x,2);s.executeUpdate();try(ResultSet k=s.getGeneratedKeys()){if(k.next())x.setId(k.getInt(1));}}return x;}
    public List<QuizQuestion> findAll()throws SQLException{String sql="SELECT "+COLUMNS+" FROM quiz_questions WHERE user_id=? ORDER BY subject_id,id";List<QuizQuestion> out=new ArrayList<>();try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,CurrentUser.get().getId());try(ResultSet r=s.executeQuery()){while(r.next())out.add(mapRow(r));}}return out;}
    public QuizQuestion findById(int id)throws SQLException{String sql="SELECT "+COLUMNS+" FROM quiz_questions WHERE id=? AND user_id=?";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,id);s.setInt(2,CurrentUser.get().getId());try(ResultSet r=s.executeQuery()){if(r.next())return mapRow(r);}}return null;}
    public void update(QuizQuestion x)throws SQLException{String sql="UPDATE quiz_questions SET subject_id=?,topic_id=?,question_text=?,option_a=?,option_b=?,option_c=?,option_d=?,correct_option=? WHERE id=? AND user_id=?";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){bind(s,x,1);s.setInt(9,x.getId());s.setInt(10,CurrentUser.get().getId());s.executeUpdate();}}
    public void delete(int id)throws SQLException{String sql="DELETE FROM quiz_questions WHERE id=? AND user_id=?";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,id);s.setInt(2,CurrentUser.get().getId());s.executeUpdate();}}
    public int countBySubjectId(int id)throws SQLException{return count("subject_id=?",id);} public int countByTopicId(int id)throws SQLException{return count("topic_id=?",id);}
    private int count(String clause,int id)throws SQLException{String sql="SELECT COUNT(*) FROM quiz_questions WHERE "+clause+" AND user_id=?";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,id);s.setInt(2,CurrentUser.get().getId());try(ResultSet r=s.executeQuery()){if(r.next())return r.getInt(1);}}return 0;}
    private void bind(PreparedStatement s,QuizQuestion q,int o)throws SQLException{s.setInt(o,q.getSubjectId());if(q.getTopicId()==null)s.setNull(o+1,Types.INTEGER);else s.setInt(o+1,q.getTopicId());s.setString(o+2,q.getQuestionText());s.setString(o+3,q.getOptionA());s.setString(o+4,q.getOptionB());s.setString(o+5,q.getOptionC());s.setString(o+6,q.getOptionD());s.setString(o+7,q.getCorrectOption().name());}
    private QuizQuestion mapRow(ResultSet r)throws SQLException{int tv=r.getInt("topic_id");Integer topic=r.wasNull()?null:tv;return new QuizQuestion(r.getInt("id"),r.getInt("subject_id"),topic,r.getString("question_text"),r.getString("option_a"),r.getString("option_b"),r.getString("option_c"),r.getString("option_d"),QuizOption.fromString(r.getString("correct_option")));}
}
