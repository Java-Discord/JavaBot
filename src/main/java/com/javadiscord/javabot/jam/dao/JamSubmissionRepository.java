package com.javadiscord.javabot.jam.dao;

import com.javadiscord.javabot.data.DatabaseHelper;
import com.javadiscord.javabot.jam.model.Jam;
import com.javadiscord.javabot.jam.model.JamSubmission;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class JamSubmissionRepository {
	private final Connection con;

	public List<JamSubmission> getSubmissions(Jam jam) throws SQLException, IOException {
		PreparedStatement stmt = con.prepareStatement(DatabaseHelper.loadSql("/jam/sql/find_latest_submissions.sql"));
		stmt.setLong(1, jam.getId());
		ResultSet rs = stmt.executeQuery();
		List<JamSubmission> submissions = new ArrayList<>();
		while (rs.next()) {
			submissions.add(this.readSubmission(rs, jam));
		}
		stmt.close();
		return submissions;
	}

	public List<JamSubmission> getSubmissions(Jam jam, int page, Long userId) throws SQLException, IOException {
		int pageSize = 10;
		String sql = "SELECT js.* FROM jam_submission js WHERE js.jam_id = ? /* CONDITIONS */ ORDER BY js.created_at LIMIT 10 /* OFFSET */"
				.replace("/* OFFSET */", "OFFSET " + (page - 1) * pageSize);
		if (userId != null) {
			sql = sql.replace("/* CONDITIONS */", "AND js.user_id = ?");
		}
		PreparedStatement stmt = con.prepareStatement(sql);
		stmt.setLong(1, jam.getId());
		if (userId != null) {
			stmt.setLong(2, userId);
		}
		ResultSet rs = stmt.executeQuery();
		List<JamSubmission> submissions = new ArrayList<>();
		while (rs.next()) {
			submissions.add(this.readSubmission(rs, jam));
		}
		stmt.close();
		return submissions;
	}

	public JamSubmission getSubmission(Jam jam, long submissionId) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("SELECT * FROM jam_submission WHERE jam_id = ? AND id = ?");
		stmt.setLong(1, jam.getId());
		stmt.setLong(2, submissionId);
		ResultSet rs = stmt.executeQuery();
		JamSubmission submission = null;
		if (rs.next()) {
			submission = this.readSubmission(rs, jam);
		}
		stmt.close();
		return submission;
	}

	public void saveSubmission(JamSubmission submission) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("INSERT INTO jam_submission (jam_id, theme_name, user_id, source_link, description) VALUES (?, ?, ?, ?, ?)");
		stmt.setLong(1, submission.getJam().getId());
		stmt.setString(2, submission.getThemeName());
		stmt.setLong(3, submission.getUserId());
		stmt.setString(4, submission.getSourceLink());
		stmt.setString(5, submission.getDescription());
		stmt.executeUpdate();
		stmt.close();
	}

	public int removeSubmission(Jam jam, long submissionId) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("DELETE FROM jam_submission WHERE jam_id = ? AND id = ?");
		stmt.setLong(1, jam.getId());
		stmt.setLong(2, submissionId);
		int rows = stmt.executeUpdate();
		stmt.close();
		return rows;
	}

	public int removeSubmissions(Jam jam, long userId) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("DELETE FROM jam_submission WHERE jam_id = ? AND user_id = ?");
		stmt.setLong(1, jam.getId());
		stmt.setLong(2, userId);
		int rows = stmt.executeUpdate();
		stmt.close();
		return rows;
	}

	private JamSubmission readSubmission(ResultSet rs, Jam jam) throws SQLException {
		JamSubmission submission = new JamSubmission();
		submission.setId(rs.getLong("id"));
		submission.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
		submission.setJam(jam);
		submission.setThemeName(rs.getString("theme_name"));
		submission.setUserId(rs.getLong("user_id"));
		submission.setSourceLink(rs.getString("source_link"));
		submission.setDescription(rs.getString("description"));
		return submission;
	}
}
