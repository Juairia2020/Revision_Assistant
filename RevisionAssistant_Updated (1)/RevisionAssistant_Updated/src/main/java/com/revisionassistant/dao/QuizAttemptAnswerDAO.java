package com.revisionassistant.dao;

import com.revisionassistant.database.DatabaseManager;
import com.revisionassistant.model.QuizAttemptAnswer;
import com.revisionassistant.model.QuizOption;
import com.revisionassistant.session.CurrentUser;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuizAttemptAnswerDAO {
    private static final String COLUMNS="id,attempt_id,question_id,selected_option,correct";
    public QuizAttemptAnswer insert(QuizAttemptAnswer a)throws SQLException{
        String sql="INSERT INTO quiz_attempt_answers(user_id,attempt_id,question_id,selected_option,correct) VALUES(?,?,?,?,?)";
        try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){
            s.setInt(1,CurrentUser.get().getId());s.setInt(2,a.getAttemptId());s.setInt(3,a.getQuestionId());s.setString(4,a.getSelectedOption().name());s.setInt(5,a.isCorrect()?1:0);s.executeUpdate();
            try(ResultSet k=s.getGeneratedKeys()){if(k.next())a.setId(k.getInt(1));}
        } return a;
    }
    public List<QuizAttemptAnswer> findByAttemptId(int id)throws SQLException{
        String sql="SELECT "+COLUMNS+" FROM quiz_attempt_answers WHERE attempt_id=? AND user_id=? ORDER BY id";return query(sql,id,false);
    }
    public List<QuizAttemptAnswer> findAllIncorrect()throws SQLException{
        String sql="SELECT "+COLUMNS+" FROM quiz_attempt_answers WHERE correct=0 AND user_id=?";List<QuizAttemptAnswer> out=new ArrayList<>();
        try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,CurrentUser.get().getId());try(ResultSet r=s.executeQuery()){while(r.next())out.add(mapRow(r));}} return out;
    }
    public int countByQuestionId(int id)throws SQLException{
        String sql="SELECT COUNT(*) FROM quiz_attempt_answers WHERE question_id=? AND user_id=?";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,id);s.setInt(2,CurrentUser.get().getId());try(ResultSet r=s.executeQuery()){if(r.next())return r.getInt(1);}}return 0;
    }
    private List<QuizAttemptAnswer> query(String sql,int id,boolean unused)throws SQLException{List<QuizAttemptAnswer> out=new ArrayList<>();try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,id);s.setInt(2,CurrentUser.get().getId());try(ResultSet r=s.executeQuery()){while(r.next())out.add(mapRow(r));}}return out;}
    private QuizAttemptAnswer mapRow(ResultSet r)throws SQLException{return new QuizAttemptAnswer(r.getInt("id"),r.getInt("attempt_id"),r.getInt("question_id"),QuizOption.fromString(r.getString("selected_option")),r.getInt("correct")==1);}
}
