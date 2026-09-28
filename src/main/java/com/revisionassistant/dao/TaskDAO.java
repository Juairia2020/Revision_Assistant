package com.revisionassistant.dao;
import com.revisionassistant.database.DatabaseManager;
import com.revisionassistant.session.CurrentUser;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.revisionassistant.model.Priority;
import com.revisionassistant.model.Task;
import com.revisionassistant.model.TaskStatus;
import java.sql.Types;
import java.time.LocalDate;

public class TaskDAO {
    private static final String COLUMNS="id,subject_id,topic_id,title,estimated_minutes,priority,deadline,completed,status";
    public Task insert(Task task)throws SQLException{String sql="INSERT INTO tasks(user_id,subject_id,topic_id,title,estimated_minutes,priority,deadline,completed,status) VALUES(?,?,?,?,?,?,?,?,?)";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){s.setInt(1,CurrentUser.get().getId());bindTask(s,task,2);s.executeUpdate();try(ResultSet k=s.getGeneratedKeys()){if(k.next())task.setId(k.getInt(1));}}return task;}
    public List<Task> findAll()throws SQLException{String sql="SELECT "+COLUMNS+" FROM tasks WHERE user_id=? ORDER BY completed,deadline IS NULL,deadline";List<Task> out=new ArrayList<>();try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,CurrentUser.get().getId());try(ResultSet r=s.executeQuery()){while(r.next())out.add(mapRow(r));}}return out;}
    public void update(Task task)throws SQLException{String sql="UPDATE tasks SET subject_id=?,topic_id=?,title=?,estimated_minutes=?,priority=?,deadline=?,completed=?,status=? WHERE id=? AND user_id=?";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){bindTask(s,task,1);s.setInt(9,task.getId());s.setInt(10,CurrentUser.get().getId());s.executeUpdate();}}
    public void updateCompleted(int id,boolean completed)throws SQLException{String sql="UPDATE tasks SET completed=?,status=? WHERE id=? AND user_id=?";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,completed?1:0);s.setString(2,completed?TaskStatus.COMPLETED.name():TaskStatus.NOT_STARTED.name());s.setInt(3,id);s.setInt(4,CurrentUser.get().getId());s.executeUpdate();}}
    public void delete(int id)throws SQLException{String sql="DELETE FROM tasks WHERE id=? AND user_id=?";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,id);s.setInt(2,CurrentUser.get().getId());s.executeUpdate();}}
    public int countBySubjectId(int id)throws SQLException{return count("subject_id=?",id);} public int countByTopicId(int id)throws SQLException{return count("topic_id=?",id);}
    private int count(String clause,int id)throws SQLException{String sql="SELECT COUNT(*) FROM tasks WHERE "+clause+" AND user_id=?";try(Connection c=DatabaseManager.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,id);s.setInt(2,CurrentUser.get().getId());try(ResultSet r=s.executeQuery()){if(r.next())return r.getInt(1);}}return 0;}
    private void bindTask(PreparedStatement s,Task t,int offset)throws SQLException{s.setInt(offset,t.getSubjectId());if(t.getTopicId()==null)s.setNull(offset+1,Types.INTEGER);else s.setInt(offset+1,t.getTopicId());s.setString(offset+2,t.getTitle());s.setInt(offset+3,t.getEstimatedMinutes());s.setString(offset+4,t.getPriority().name());s.setString(offset+5,t.getDeadline()==null?null:t.getDeadline().toString());s.setInt(offset+6,t.isCompleted()?1:0);s.setString(offset+7,t.getStatus().name());}
    private Task mapRow(ResultSet r)throws SQLException{int tv=r.getInt("topic_id");Integer topic=r.wasNull()?null:tv;String d=r.getString("deadline");TaskStatus status=TaskStatus.fromString(r.getString("status"));return new Task(r.getInt("id"),r.getInt("subject_id"),topic,r.getString("title"),r.getInt("estimated_minutes"),Priority.fromString(r.getString("priority")),d==null?null:LocalDate.parse(d),status);}
}
