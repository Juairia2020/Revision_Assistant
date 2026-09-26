package com.revisionassistant.dao;
import com.revisionassistant.database.DatabaseManager;
import com.revisionassistant.session.CurrentUser;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.revisionassistant.model.QuizAttempt;
import java.sql.Types;
import java.time.LocalDate;

public class QuizAttemptDAO {
    private static final String COLUMNS="id,subject_id,topic_id,attempt_date,total_questions,correct_answers,score_percent";
    public QuizAttempt insert(QuizAttempt x)throws SQLException{String sql="INSERT INTO quiz_attempts(user_id,subject_id,topic_id,attempt_date,total_questions,correct_answers,score_percent) VALUES(?,?,?,?,?,?,?)";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){s.setInt(1,CurrentUser.get().getId());if(x.getTopicId()==null)s.setNull(3,Types.INTEGER);else s.setInt(3,x.getTopicId());s.setInt(2,x.getSubjectId());s.setString(4,x.getAttemptDate().toString());s.setInt(5,x.getTotalQuestions());s.setInt(6,x.getCorrectAnswers());s.setInt(7,x.getScorePercent());s.executeUpdate();try(ResultSet k=s.getGeneratedKeys()){if(k.next())x.setId(k.getInt(1));}}return x;}
    public List<QuizAttempt> findAll()throws SQLException{String sql="SELECT "+COLUMNS+" FROM quiz_attempts WHERE user_id=? ORDER BY id DESC";List<QuizAttempt> out=new ArrayList<>();try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,CurrentUser.get().getId());try(ResultSet r=s.executeQuery()){while(r.next())out.add(mapRow(r));}}return out;}
    public int countBySubjectId(int id)throws SQLException{return count("subject_id=?",id);} public int countByTopicId(int id)throws SQLException{return count("topic_id=?",id);}
    private int count(String clause,int id)throws SQLException{String sql="SELECT COUNT(*) FROM quiz_attempts WHERE "+clause+" AND user_id=?";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,id);s.setInt(2,CurrentUser.get().getId());try(ResultSet r=s.executeQuery()){if(r.next())return r.getInt(1);}}return 0;}
    private QuizAttempt mapRow(ResultSet r)throws SQLException{int tv=r.getInt("topic_id");Integer topic=r.wasNull()?null:tv;return new QuizAttempt(r.getInt("id"),r.getInt("subject_id"),topic,LocalDate.parse(r.getString("attempt_date")),r.getInt("total_questions"),r.getInt("correct_answers"),r.getInt("score_percent"));}
}
