package osa.ora.customer.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import osa.ora.DBConnection;
import osa.ora.customer.exception.JsonMessage;
import osa.ora.beans.BankAccount;

public class BankAccountPersistence {

	private Connection conn = null;

	public BankAccountPersistence() {
		conn = DBConnection.getInstance().getConnection();

	}

	public JsonMessage save(BankAccount account) {
		String insertTableSQL = "INSERT INTO accounts "
				+ "(Account_no, Balance, currency) "
				+ "VALUES(?,?,?)";

		try (PreparedStatement preparedStatement = this.conn
				.prepareStatement(insertTableSQL)) {

			preparedStatement.setString(1, account.getAccount_no());
			preparedStatement.setDouble(2, account.getBalance());
			preparedStatement.setString(3, account.getCurrency());

			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			return new JsonMessage("Error", "Customer add failed: "
					+ e.getMessage());
		} catch (Exception e) {
			return new JsonMessage("Error", "Customer add failed: "
					+ e.getMessage());
		}
		return new JsonMessage("Success", "Customer add succeeded...");
	}

	public boolean update(BankAccount account) {
		String updateTableSQL = "UPDATE Accounts SET BALANCE=? WHERE ACCOUNT_NO=?";
		try (PreparedStatement preparedStatement = this.conn
				.prepareStatement(updateTableSQL);) {
			preparedStatement.setDouble(1, account.getBalance());
			preparedStatement.setString(2, account.getAccount_no());
			preparedStatement.executeUpdate();
                        return true;
		} catch (SQLException e) {
                    e.printStackTrace();
		} catch (Exception e) {
                    e.printStackTrace();
		}
		return false;
	}

	public JsonMessage delete(String id) {
		String deleteRowSQL = "DELETE FROM Accounts WHERE ACCOUNT_NO='?'";
		try (PreparedStatement preparedStatement = this.conn
				.prepareStatement(deleteRowSQL)) {
			preparedStatement.setString(1, id);
			preparedStatement.executeUpdate();

		} catch (SQLException e) {
			return new JsonMessage("Error", "Bank Account delete failed: "
					+ e.getMessage());
		} catch (Exception e) {
			return new JsonMessage("Error", "Bank Account delete failed: "
					+ e.getMessage());
		}
		return new JsonMessage("Success", "Bank Account delete succeeded...");
	}

	public BankAccount[] findAll() {
		String queryStr = "SELECT * FROM Accounts";
		return this.query(queryStr);
	}

	public BankAccount findbyId(String account_no) {
		String queryStr = "SELECT * FROM Accounts WHERE account_no='" + account_no+"'";
		BankAccount customer = null;
		BankAccount customers[] = this.query(queryStr);
		if (customers != null && customers.length > 0) {
			customer = customers[0];
		}
		return customer;
	}

	public BankAccount[] query(String sqlQueryStr) {
		ArrayList<BankAccount> cList = new ArrayList<>();
		try (PreparedStatement stmt = conn.prepareStatement(sqlQueryStr)) {
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				cList.add(new BankAccount(rs.getInt("ID"), rs
						.getString("ACCOUNT_NO"), rs.getDouble("BALANCE"), rs
						.getString("CURRENCY")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cList.toArray(new BankAccount[0]);
	}
}
