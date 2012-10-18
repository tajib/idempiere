package org.compiere.grid;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;

import org.compiere.model.GridTab;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrder;
import org.compiere.model.MPayment;
import org.compiere.model.MRole;
import org.compiere.process.DocAction;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;

public abstract class PaymentFormCheck extends PaymentForm {
	public static final String PAYMENTRULE = MInvoice.PAYMENTRULE_Check;
	
	public PaymentFormCheck(int WindowNo, GridTab mTab) {
		super(WindowNo, mTab);
	}
	
	public KeyNamePair selectedBankAccount;
	public ArrayList<KeyNamePair> getBankAccountList() {
		selectedBankAccount = null;
		ArrayList<KeyNamePair> list = new ArrayList<KeyNamePair>();
		
		/**
		 *  Load Bank Accounts
		 */
		String SQL = MRole.getDefault().addAccessSQL(
			"SELECT C_BankAccount_ID, ba.Name || ' ' || AccountNo, IsDefault "
			+ "FROM C_BankAccount ba"
			+ " INNER JOIN C_Bank b ON (ba.C_Bank_ID=b.C_Bank_ID) "
			+ "WHERE b.IsActive='Y'",
			"ba", MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO);
		try
		{
			PreparedStatement pstmt = DB.prepareStatement(SQL, null);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next())
			{
				int key = rs.getInt(1);
				String name = rs.getString(2);
				KeyNamePair pp = new KeyNamePair(key, name);
				list.add(pp);
				if (key == m_C_BankAccount_ID)
					selectedBankAccount = pp;
				if (selectedBankAccount == null && rs.getString(3).equals("Y"))    //  Default
					selectedBankAccount = pp;
			}
			rs.close();
			pstmt.close();
		}
		catch (SQLException ept)
		{
			log.log(Level.SEVERE, SQL, ept);
		}
		
		return list;
	}
	
	public String processMsg = null;
	public boolean save(int newC_BankAccount_ID, String routing, String number, String check, BigDecimal amount)
	{
		processMsg = null;
		boolean error = false;
		String payTypes = m_Cash_As_Payment ? "KTSDB" : "KTSD";
		
		/***********************
		 *  Changed PaymentRule
		 */
		if (!PAYMENTRULE.equals(m_PaymentRule))
		{
			log.fine("Changed PaymentRule: " + m_PaymentRule + " -> " + PAYMENTRULE);
			//  We had a change in Payment type (e.g. Check to CC)
			if (payTypes.indexOf(m_PaymentRule) != -1 && payTypes.indexOf(PAYMENTRULE) != -1 && m_mPaymentOriginal != null)
			{
				log.fine("Old Payment(1) - " + m_mPaymentOriginal);
				m_mPaymentOriginal.setDocAction(DocAction.ACTION_Reverse_Correct);
				boolean ok = m_mPaymentOriginal.processIt(DocAction.ACTION_Reverse_Correct);
				m_mPaymentOriginal.saveEx();
				if (ok)
					log.info( "Payment Canecelled - " + m_mPaymentOriginal);
				else
				{
					processMsg = "PaymentNotCancelled " + m_mPaymentOriginal.getDocumentNo();
					error = true;
				}
				m_mPayment.resetNew();
			}
			//	We had a Payment and something else (e.g. Check to Cash)
			else if (payTypes.indexOf(m_PaymentRule) != -1 && payTypes.indexOf(PAYMENTRULE) == -1)
			{
				log.fine("Old Payment(2) - " + m_mPaymentOriginal);
				if (m_mPaymentOriginal != null)
				{
					m_mPaymentOriginal.setDocAction(DocAction.ACTION_Reverse_Correct);
					boolean ok = m_mPaymentOriginal.processIt(DocAction.ACTION_Reverse_Correct);
					m_mPaymentOriginal.saveEx();
					if (ok)        //  Cancel Payment
					{
						log.fine("PaymentCancelled " + m_mPayment.getDocumentNo ());
						getGridTab().getTableModel().dataSave(true);
						m_mPayment.resetNew();
						m_mPayment.setAmount(m_C_Currency_ID, m_Amount);
					}
					else
					{
						processMsg = "PaymentNotCancelled " + m_mPayment.getDocumentNo();
						error = true;
					}
				}
			}
		}
		
		//  Get Order and optionally Invoice
		int C_Order_ID = Env.getContextAsInt(Env.getCtx(), getWindowNo(), "C_Order_ID");
		int C_Invoice_ID = Env.getContextAsInt(Env.getCtx(), getWindowNo(), "C_Invoice_ID");
		if (C_Invoice_ID == 0 && m_DocStatus.equals("CO"))
			C_Invoice_ID = getInvoiceID (C_Order_ID);

		//  Amount sign negative, if ARC (Credit Memo) or API (AP Invoice)
		boolean negateAmt = false;
		MInvoice invoice = null;
		if (C_Invoice_ID != 0)
		{
			invoice = new MInvoice (Env.getCtx(), C_Invoice_ID, null);
			negateAmt = invoice.isCreditMemo();
		}
		MOrder order = null;
		if (invoice == null && C_Order_ID != 0)
			order = new MOrder (Env.getCtx(), C_Order_ID, null);
		
		BigDecimal payAmount = m_Amount;
		

		if (negateAmt)
			payAmount = m_Amount.negate();
		// Info
		log.config("C_Order_ID=" + C_Order_ID + ", C_Invoice_ID=" + C_Invoice_ID + ", NegateAmt=" + negateAmt);
		
		/***********************
		 *  Payments
		 */
		log.fine("Payment - " + PAYMENTRULE);
		//  Set Amount
		m_mPayment.setAmount(m_C_Currency_ID, payAmount);
		m_mPayment.setBankCheck(newC_BankAccount_ID, m_isSOTrx, routing,
			number, check);
		// Get changes to check amount
		m_mPayment.setAmount(m_C_Currency_ID, amount);
		m_mPayment.setC_BPartner_ID(m_C_BPartner_ID);
		m_mPayment.setC_Invoice_ID(C_Invoice_ID);
		if (order != null)
		{
			m_mPayment.setC_Order_ID(C_Order_ID);
			m_needSave = true;
		}
		m_mPayment.setDateTrx(m_DateAcct);
		m_mPayment.setDateAcct(m_DateAcct);
		m_mPayment.saveEx();
		
		//  Save/Post
		if (m_mPayment.get_ID() > 0 && MPayment.DOCSTATUS_Drafted.equals(m_mPayment.getDocStatus()))
		{
			boolean ok = m_mPayment.processIt(DocAction.ACTION_Complete);
			m_mPayment.saveEx();
			if (ok)
				processMsg = m_mPayment.getDocumentNo();
			else
			{
				processMsg = "PaymentNotCreated";
				error = true;
			}
		}
		else
			log.fine("NotDraft " + m_mPayment);
		
		/**********************
		 *	Save Values to mTab
		 */
		log.config("Saving changes");
		//	Set Payment
		if (m_mPayment.getC_Payment_ID() != m_C_Payment_ID)
		{
			if (m_mPayment.getC_Payment_ID() == 0)
				getGridTab().setValue("C_Payment_ID", null);
			else
				getGridTab().setValue("C_Payment_ID", new Integer(m_mPayment.getC_Payment_ID()));
		}
		return !error;
	}
}
