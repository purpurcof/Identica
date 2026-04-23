package me.whereareiam.identica.adapter.database.config;

import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.logging.LoggingHelper;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.SqlLogger;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.core.statement.StatementContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Database Logger Configuration")
class LoggerConfigTest {
	@AfterEach
	void tearDown() {
		Logger.init(null);
	}

	@DisplayName("Logs SQL execution at trace level before and after the statement runs")
	@Test
	void configuresTraceLoggingForSqlExecution() {
		Jdbi jdbi = mock(Jdbi.class);
		SqlStatements sqlStatements = mock(SqlStatements.class);
		when(jdbi.getConfig(SqlStatements.class)).thenReturn(sqlStatements);
		LogCapture loggingHelper = new LogCapture();
		Logger.init(loggingHelper);

		LoggerConfig.configure(jdbi);
		SqlLogger sqlLogger = capture(sqlStatements);
		StatementContext context = mock(StatementContext.class);
		when(context.getRenderedSql()).thenReturn("select 1");

		sqlLogger.logBeforeExecution(context);
		sqlLogger.logAfterExecution(context);

		assertEquals("Executing SQL: select 1", loggingHelper.beforeExecutionMessage);
		assertEquals("SQL completed: select 1", loggingHelper.afterExecutionMessage);
	}

	@DisplayName("Keeps SQL exceptions at warn level")
	@Test
	void keepsSqlExceptionsAtWarnLevel() {
		Jdbi jdbi = mock(Jdbi.class);
		SqlStatements sqlStatements = mock(SqlStatements.class);
		when(jdbi.getConfig(SqlStatements.class)).thenReturn(sqlStatements);
		LogCapture loggingHelper = new LogCapture();
		Logger.init(loggingHelper);

		LoggerConfig.configure(jdbi);
		SqlLogger sqlLogger = capture(sqlStatements);
		StatementContext context = mock(StatementContext.class);
		when(context.getRenderedSql()).thenReturn("select 1");

		sqlLogger.logException(context, new SQLException("boom"));

		assertEquals("SQL error [select 1]: boom", loggingHelper.warnMessage);
	}

	private SqlLogger capture(SqlStatements sqlStatements) {
		org.mockito.ArgumentCaptor<SqlLogger> captor =
				org.mockito.ArgumentCaptor.forClass(SqlLogger.class);
		verify(sqlStatements).setSqlLogger(captor.capture());
		SqlLogger sqlLogger = captor.getValue();
		assertNotNull(sqlLogger);
		return sqlLogger;
	}

	private static final class LogCapture implements LoggingHelper {
		private String beforeExecutionMessage;
		private String afterExecutionMessage;
		private String warnMessage;

		@Override
		public void info(String message, Object... objects) {
		}

		@Override
		public void warn(String message, Object... objects) {
			warnMessage = format(message, objects);
		}

		@Override
		public void severe(String message, Object... objects) {
		}

		@Override
		public void debug(String message, Object... objects) {
		}

		@Override
		public void trace(String message, Object... objects) {
			String formatted = format(message, objects);
			if (formatted.startsWith("Executing SQL:")) {
				beforeExecutionMessage = formatted;
				return;
			}

			afterExecutionMessage = formatted;
		}

		private String format(String message, Object... objects) {
			return objects == null || objects.length == 0 ? message : String.format(message, objects);
		}
	}
}
