package com.revisionassistant.dao;
import com.revisionassistant.database.DatabaseManager;
import com.revisionassistant.session.CurrentUser;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.revisionassistant.model.StudySession;
import java.sql.Types;
import java.time.LocalDate;

public class StudySessionDAO {
    private static final String COLUMNS="id,subject_id,topic_id,session_date,duration_minutes,notes";
    public StudySession insert(StudySession x)throws SQLException{String sql="INSERT INTO study_sessions(user_id,subject_id,topic_id,session_date,duration_minutes,notes) VALUES(?,?,?,?,?,?)";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){bind(s,x,2);s.setInt(1,CurrentUser.get().getId());s.executeUpdate();try(ResultSet k=s.getGeneratedKeys()){if(k.next())x.setId(k.getInt(1));}}return x;}
    public List<StudySession> findAll()throws SQLException{String sql="SELECT "+COLUMNS+" FROM study_sessions WHERE user_id=? ORDER BY session_date DESC,id DESC";List<StudySession> out=new ArrayList<>();try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,CurrentUser.get().getId());try(ResultSet r=s.executeQuery()){while(r.next())out.add(mapRow(r));}}return out;}
    public void update(StudySession x)throws SQLException{String sql="UPDATE study_sessions SET subject_id=?,topic_id=?,session_date=?,duration_minutes=?,notes=? WHERE id=? AND user_id=?";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){bind(s,x,1);s.setInt(6,x.getId());s.setInt(7,CurrentUser.get().getId());s.executeUpdate();}}
    public void delete(int id)throws SQLException{String sql="DELETE FROM study_sessions WHERE id=? AND user_id=?";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,id);s.setInt(2,CurrentUser.get().getId());s.executeUpdate();}}
    public int countBySubjectId(int id)throws SQLException{return count("subject_id=?",id);} public int countByTopicId(int id)throws SQLException{return count("topic_id=?",id);}
    private int count(String clause,int id)throws SQLException{String sql="SELECT COUNT(*) FROM study_sessions WHERE "+clause+" AND user_id=?";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,id);s.setInt(2,CurrentUser.get().getId());try(ResultSet r=s.executeQuery()){if(r.next())return r.getInt(1);}}return 0;}
    private void bind(PreparedStatement s,StudySession x,int o)throws SQLException{s.setInt(o,x.getSubjectId());if(x.getTopicId()==null)s.setNull(o+1,Types.INTEGER);else s.setInt(o+1,x.getTopicId());s.setString(o+2,x.getDate().toString());s.setInt(o+3,x.getDurationMinutes());s.setString(o+4,x.getNotes());}
    private StudySession mapRow(ResultSet r)throws SQLException{int tv=r.getInt("topic_id");Integer topic=r.wasNull()?null:tv;return new StudySession(r.getInt("id"),r.getInt("subject_id"),topic,LocalDate.parse(r.getString("session_date")),r.getInt("duration_minutes"),r.getString("notes"));}
}
