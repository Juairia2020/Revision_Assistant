package com.revisionassistant.dao;
import com.revisionassistant.database.DatabaseManager;
import com.revisionassistant.session.CurrentUser;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.revisionassistant.model.Exam;
import java.time.LocalDate;

public class ExamDAO {
    private static final String COLUMNS="id,subject_id,title,exam_date,progress";
    public Exam insert(Exam exam)throws SQLException{String sql="INSERT INTO exams(user_id,subject_id,title,exam_date,progress) VALUES(?,?,?,?,?)";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){s.setInt(1,CurrentUser.get().getId());bind(s,exam,2);s.executeUpdate();try(ResultSet k=s.getGeneratedKeys()){if(k.next())exam.setId(k.getInt(1));}}return exam;}
    public List<Exam> findAll()throws SQLException{String sql="SELECT "+COLUMNS+" FROM exams WHERE user_id=? ORDER BY exam_date";List<Exam> out=new ArrayList<>();try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,CurrentUser.get().getId());try(ResultSet r=s.executeQuery()){while(r.next())out.add(mapRow(r));}}return out;}
    public void update(Exam exam)throws SQLException{String sql="UPDATE exams SET subject_id=?,title=?,exam_date=?,progress=? WHERE id=? AND user_id=?";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){bind(s,exam,1);s.setInt(5,exam.getId());s.setInt(6,CurrentUser.get().getId());s.executeUpdate();}}
    public void delete(int id)throws SQLException{String sql="DELETE FROM exams WHERE id=? AND user_id=?";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,id);s.setInt(2,CurrentUser.get().getId());s.executeUpdate();}}
    public int countBySubjectId(int id)throws SQLException{String sql="SELECT COUNT(*) FROM exams WHERE subject_id=? AND user_id=?";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,id);s.setInt(2,CurrentUser.get().getId());try(ResultSet r=s.executeQuery()){if(r.next())return r.getInt(1);}}return 0;}
    private void bind(PreparedStatement s,Exam e,int o)throws SQLException{s.setInt(o,e.getSubjectId());s.setString(o+1,e.getTitle());s.setString(o+2,e.getExamDate().toString());s.setInt(o+3,e.getProgress());}
    private Exam mapRow(ResultSet r)throws SQLException{return new Exam(r.getInt("id"),r.getInt("subject_id"),r.getString("title"),LocalDate.parse(r.getString("exam_date")),r.getInt("progress"));}
}
