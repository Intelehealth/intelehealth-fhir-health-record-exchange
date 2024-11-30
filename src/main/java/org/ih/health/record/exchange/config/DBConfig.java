package org.ih.health.record.exchange.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
public class DBConfig {

	@Value("${spring.secondDatasource.driverClassName}")
	private String dbDriver;

	@Value("${spring.secondDatasource.url}")
	private String dbUrl;

	@Value("${spring.secondDatasource.username}")
	private String dbUsername;

	@Value("${spring.secondDatasource.password}")
	private String dbPassword;

	public DataSource dataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(dbDriver);
		dataSource.setUrl(dbUrl);
		dataSource.setUsername(dbUsername);
		dataSource.setPassword(dbPassword);
		return dataSource;
	}

	public JdbcTemplate template() {
		return new JdbcTemplate(dataSource());
	}

	public NamedParameterJdbcTemplate namedTemplate() {
		return new NamedParameterJdbcTemplate(dataSource());
	}
}