package com.revisionassistant.dao;
import com.revisionassistant.database.DatabaseManager;
import com.revisionassistant.session.CurrentUser;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.revisionassistant.model.Subject;

public class SubjectDAO {
    public Subject insert(Subject subject) throws SQLException {
        String sql = "INSERT INTO subjects (user_id, name, color) VALUES (?, ?, ?)";
        try (Connection connection = DatabaseManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, CurrentUser.get().getId()); statement.setString(2, subject.getName()); statement.setString(3, subject.getColor()); statement.executeUpdate();
            try (ResultSet keys=statement.getGeneratedKeys()) { if(keys.next()) subject.setId(keys.getInt(1)); }
        } return subject;
    }
    public List<Subject> findAll() throws SQLException {
        String sql="SELECT id,name,color FROM subjects WHERE user_id=? ORDER BY name"; List<Subject> out=new ArrayList<>();
        try(Connection c=DatabaseManager.getConnection(); PreparedStatement s=c.prepareStatement(sql)){ s.setInt(1,CurrentUser.get().getId()); try(ResultSet r=s.executeQuery()){while(r.next())out.add(mapRow(r));} } return out;
    }
    public Subject findById(int id) throws SQLException {
        String sql="SELECT id,name,color FROM subjects WHERE id=? AND user_id=?"; try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,id);s.setInt(2,CurrentUser.get().getId());try(ResultSet r=s.executeQuery()){if(r.next())return mapRow(r);}} return null;
    }
    public void update(Subject subject) throws SQLException {
        String sql="UPDATE subjects SET name=?,color=? WHERE id=? AND user_id=?"; try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setString(1,subject.getName());s.setString(2,subject.getColor());s.setInt(3,subject.getId());s.setInt(4,CurrentUser.get().getId());s.executeUpdate();}
    }
    public void delete(int id) throws SQLException {
        String sql="DELETE FROM subjects WHERE id=? AND user_id=?"; try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,id);s.setInt(2,CurrentUser.get().getId());s.executeUpdate();}
    }
    private Subject mapRow(ResultSet r)throws SQLException{return new Subject(r.getInt("id"),r.getString("name"),r.getString("color"));}
}
