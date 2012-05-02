/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2008 Low Heng Sin. All Rights Reserved.                      *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package org.adempiere.webui.window;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Vector;
import java.util.logging.Level;

import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Checkbox;
import org.adempiere.webui.component.FolderBrowser;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ListHead;
import org.adempiere.webui.component.ListHeader;
import org.adempiere.webui.component.Listbox;
import org.adempiere.webui.component.ListboxFactory;
import org.adempiere.webui.component.SimpleListModel;
import org.adempiere.webui.component.Tab;
import org.adempiere.webui.component.Tabbox;
import org.adempiere.webui.component.Tabpanel;
import org.adempiere.webui.component.Tabpanels;
import org.adempiere.webui.component.Tabs;
import org.adempiere.webui.component.ToolBarButton;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.theme.ThemeManager;
import org.compiere.Adempiere;
import org.compiere.model.MUser;
import org.compiere.util.CLogErrorBuffer;
import org.compiere.util.CLogMgt;
import org.compiere.util.Env;
import org.compiere.util.Ini;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.zkoss.util.media.AMedia;
import org.zkoss.zhtml.Pre;
import org.zkoss.zhtml.Text;
import org.zkoss.zhtml.Textarea;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.MaximizeEvent;
import org.zkoss.zk.ui.event.SizeEvent;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Separator;
import org.zkoss.zul.Space;
import org.zkoss.zul.Vbox;

/**
 *
 * @author Low Heng Sin
 *
 */
public class AboutWindow extends Window implements EventListener {

	/**
	 * generated serial version id
	 */
	private static final long serialVersionUID = -257313771447940626L;
	private Checkbox bErrorsOnly;
	private Listbox logTable;
	private Tabbox tabbox;
	private Tabpanels tabPanels;
	private Button btnDownload;
	private Button btnErrorEmail;
	private Button btnViewLog;
	private Tab tabLog;
	
	private Button btnAdempiereLog;

	private Button btnServerLog;

	private Listbox levelListBox;

	public AboutWindow() {
		super();
		init();
	}

	private void init() {

		this.setPosition("center");
		this.setTitle(ThemeManager.getBrowserTitle());
		this.setClosable(true);
		this.setMaximizable(true);
		this.setSizable(true);

		this.addEventListener(Events.ON_SIZE, this);
		this.addEventListener(Events.ON_MAXIMIZE, this);

		Vbox layout = new Vbox();
		layout.setWidth("100%");
		layout.setParent(this);

		tabbox = new Tabbox();
		tabbox.setParent(layout);
		tabbox.setWidth("480px");
		tabbox.setHeight("380px");
//		tabbox.setSclass("lite");
		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);
		tabPanels = new Tabpanels();
		tabPanels.setParent(tabbox);
		tabPanels.setWidth("480px");

		//about
		Tab tab = new Tab();
		tab.setLabel(Msg.getMsg(Env.getCtx(), "About"));
		tab.setParent(tabs);
		Tabpanel tabPanel = createAbout();
		tabPanel.setParent(tabPanels);

		//Credit
		tab = new Tab();
		tab.setLabel(Msg.getMsg(Env.getCtx(), "Credits"));
		tab.setParent(tabs);
		tabPanel = createCredit();
		tabPanel.setParent(tabPanels);

		//Info
		tab = new Tab();
		tab.setLabel(Msg.getMsg(Env.getCtx(), "Info"));
		tab.setParent(tabs);
		tabPanel = createInfo();
		tabPanel.setParent(tabPanels);

		//Trace
		tab = new Tab();
		tab.setLabel("Errors");
		tabLog = tab;
		tab.setParent(tabs);
		tabPanel = createTrace();
		tabPanel.setParent(tabPanels);

		Hbox hbox = new Hbox();
		hbox.setParent(layout);
		hbox.setPack("end");
		hbox.setWidth("100%");
		Button btnOk = new Button();
		btnOk.setImage("/images/Ok24.png");
		btnOk.addEventListener(Events.ON_CLICK, this);
		btnOk.setParent(hbox);

		this.setBorder("normal");
		this.setWidth("500px");
		this.setHeight("450px");
		doResize(500, 450);
	}

	private Tabpanel createTrace() {
		Tabpanel tabPanel = new Tabpanel();
		Vbox vbox = new Vbox();
		vbox.setParent(tabPanel);
		vbox.setWidth("100%");
		vbox.setHeight("100%");

		Hbox hbox = new Hbox();
		bErrorsOnly = new Checkbox();
		bErrorsOnly.setLabel(Msg.getMsg(Env.getCtx(), "ErrorsOnly"));
		//default only show error
		bErrorsOnly.setChecked(true);
		bErrorsOnly.addEventListener(Events.ON_CHECK, this);
		hbox.appendChild(bErrorsOnly);
		hbox.appendChild(new Space());
		btnDownload = new Button(Msg.getMsg(Env.getCtx(), "SaveFile"));
		btnDownload .setTooltiptext("Download session log");
		btnDownload.addEventListener(Events.ON_CLICK, this);
		hbox.appendChild(btnDownload);
		hbox.appendChild(new Space());
		btnErrorEmail = new Button(Msg.getMsg(Env.getCtx(), "SendEMail"));
		btnErrorEmail.setTooltiptext("Email session log");
		btnErrorEmail.addEventListener(Events.ON_CLICK, this);
		hbox.appendChild(btnErrorEmail);
		hbox.appendChild(new Space());
		btnViewLog = new Button(Util.cleanAmp(Msg.getMsg(Env.getCtx(), "View")));
		btnViewLog.setTooltiptext("View session log");
		btnViewLog.addEventListener(Events.ON_CLICK, this);
		hbox.appendChild(btnViewLog);
		vbox.appendChild(hbox);

		hbox = new Hbox();
		hbox.setAlign("center");
		hbox.setPack("start");
		Label levelLabel = new Label("Trace Level:");
		levelLabel.setHeight("100%");
		hbox.appendChild(levelLabel);
		levelListBox = ListboxFactory.newDropdownListbox();
		levelListBox.addEventListener(Events.ON_SELECT, this);
		hbox.appendChild(levelListBox);
		for (Level level : CLogMgt.LEVELS)
		{
			levelListBox.appendItem(level.getName(), level);
		}

		Level level = CLogMgt.getLevel();
		for (int i = 0; i < CLogMgt.LEVELS.length; i++)
		{
			if (CLogMgt.LEVELS[i].intValue() == level.intValue())
			{
				levelListBox.setSelectedIndex(i);
				break;
			}
		}

		levelListBox.setEnabled(false);
		if (Env.getAD_Client_ID(Env.getCtx()) == 0)
		{
			MUser user = MUser.get(Env.getCtx());
			if (user.isAdministrator())
			{
				levelListBox.setEnabled(true);
				levelListBox.setTooltiptext("Set trace level. Warning: this will effect all session not just the current session");
				levelLabel.setTooltiptext("Set trace level. Warning: this will effect all session not just the current session");
				btnAdempiereLog = new Button("Adempiere Log");
				btnAdempiereLog.setTooltiptext("Download adempiere log file from server");
				btnAdempiereLog.addEventListener(Events.ON_CLICK, this);

				btnServerLog = new Button("JBoss Log");
				btnServerLog.setTooltiptext("Download JBoss console log file from server");
				btnServerLog.addEventListener(Events.ON_CLICK, this);

				hbox.appendChild(new Space());
				hbox.appendChild(btnAdempiereLog);
				hbox.appendChild(new Space());
				hbox.appendChild(btnServerLog);

			}
		}

		vbox.appendChild(hbox);

		Vector<String> columnNames = CLogErrorBuffer.get(true).getColumnNames(Env.getCtx());

		logTable = new Listbox();
		ListHead listHead = new ListHead();
		listHead.setParent(logTable);
		listHead.setSizable(true);
		for (Object obj : columnNames) {
			ListHeader header = new ListHeader(obj.toString());
			header.setWidth("100px");
			listHead.appendChild(header);
		}

		vbox.appendChild(logTable);
		logTable.setWidth("480px");
		logTable.setHeight("300px");
		logTable.setVflex(false);

		updateLogTable();

		return tabPanel;
	}

	private void updateLogTable() {
		Vector<Vector<Object>> data = CLogErrorBuffer.get(true).getLogData(bErrorsOnly.isChecked());
		SimpleListModel model = new SimpleListModel(data);
		model.setMaxLength(new int[]{0, 0, 0, 200, 0, 200});
		logTable.setItemRenderer(model);
		logTable.setModel(model);
		if (bErrorsOnly.isSelected())
			tabLog.setLabel(Msg.getMsg(Env.getCtx(), "Errors") + " (" + data.size() + ")");
		else
			tabLog.setLabel(Msg.getMsg(Env.getCtx(), "TraceInfo") + " (" + data.size() + ")");
	}

	private Tabpanel createInfo() {
		Tabpanel tabPanel = new Tabpanel();
		Div div = new Div();
		div.setParent(tabPanel);
		div.setHeight("100%");
		div.setStyle("overflow: auto;");
		Pre pre = new Pre();
		pre.setParent(div);
		Text text = new Text(CLogMgt.getInfo(null).toString());
		text.setParent(pre);

		return tabPanel;
	}

	private Tabpanel createCredit() {
		Tabpanel tabPanel = new Tabpanel();
		Vbox vbox = new Vbox();
		vbox.setParent(tabPanel);
		vbox.setWidth("100%");
		Hbox hbox = new Hbox();
		hbox.setParent(vbox);
		ToolBarButton link = new ToolBarButton();
		link.setImage("images/Posterita.jpg");
		link.setParent(hbox);
		link.setHref("http://www.posterita.org");
		link.setTarget("_blank");
		Label label= new Label("Contributed the initial Zk Web Client code.");
		label.setParent(hbox);

		Separator separator = new Separator();
		separator.setParent(vbox);

		Div div = new Div();
		div.setParent(vbox);
		div.setWidth("100%");
		Label caption = new Label("Sponsors");
		caption.setStyle("font-weight: bold;");
		div.appendChild(caption);
		separator = new Separator();
		separator.setBar(true);
		separator.setParent(div);
		Vbox content = new Vbox();
		content.setWidth("100%");
		content.setParent(div);
		link = new ToolBarButton();
		link.setLabel("Sysnova");
		link.setHref("http://www.sysnova.com/");
		link.setTarget("_blank");
		link.setParent(content);
		link = new ToolBarButton();
		link.setLabel("Idalica");
		link.setHref("http://www.idalica.com/");
		link.setTarget("_blank");
		link.setParent(content);

		separator = new Separator();
		separator.setParent(vbox);

		div = new Div();
		div.setParent(vbox);
		div.setWidth("100%");
		caption = new Label("Contributors");
		caption.setStyle("font-weight: bold;");
		div.appendChild(caption);
		separator = new Separator();
		separator.setBar(true);
		separator.setParent(div);
		content = new Vbox();
		content.setWidth("100%");
		content.setParent(div);
		link = new ToolBarButton();
		link.setLabel("Ashley G Ramdass");
		link.setHref("http://www.adempiere.com/wiki/index.php/User:Agramdass");
		link.setTarget("_blank");
		link.setParent(content);

		link = new ToolBarButton();
		link.setLabel("Low Heng Sin");
		link.setHref("http://www.adempiere.com/wiki/index.php/User:Hengsin");
		link.setTarget("_blank");
		link.setParent(content);

		link = new ToolBarButton();
		link.setLabel("Carlos Ruiz");
		link.setHref("http://www.adempiere.com/wiki/index.php/User:CarlosRuiz");
		link.setTarget("_blank");
		link.setParent(content);

		link = new ToolBarButton();
		link.setLabel("Teo Sarca");
		link.setHref("http://www.adempiere.com/wiki/index.php/User:Teo_sarca");
		link.setTarget("_blank");
		link.setParent(content);

		link = new ToolBarButton();
		link.setLabel("Trifon Trifonov");
		link.setHref("http://www.adempiere.com/wiki/index.php/User:Trifonnt");
		link.setTarget("_blank");
		link.setParent(content);

		return tabPanel;
	}

	private Tabpanel createAbout() {
		Tabpanel tabPanel = new Tabpanel();

		Vbox vbox = new Vbox();
		vbox.setWidth("100%");
		vbox.setHeight("100%");
		vbox.setAlign("center");
		vbox.setPack("center");
		vbox.setParent(tabPanel);

		Image image = new Image(ThemeManager.getSmallLogo());
		image.setParent(vbox);

		Text text = new Text(Adempiere.getSubtitle());
		text.setParent(vbox);
		Separator separator = new Separator();
		separator.setParent(vbox);

		text = new Text(Adempiere.getVersion());
		text.setParent(vbox);

		separator = new Separator();
		separator.setParent(vbox);
		ToolBarButton link = new ToolBarButton();
		link.setLabel("Sourceforge.net Project Site");
		link.setHref("http://www.sourceforge.net/projects/adempiere");
		link.setTarget("_blank");
		link.setParent(vbox);

		separator = new Separator();
		separator.setParent(vbox);
		link = new ToolBarButton();
		link.setLabel("ADempiere Wiki");
		link.setHref("http://www.adempiere.com/wiki/index.php");
		link.setTarget("_blank");
		link.setParent(vbox);

		separator = new Separator();
		separator.setParent(vbox);
		link = new ToolBarButton();
		link.setLabel("ADempiere.org");
		link.setHref("http://www.adempiere.org");
		link.setTarget("_blank");
		link.setParent(vbox);

		separator = new Separator();
		separator.setParent(vbox);
		link = new ToolBarButton();
		link.setLabel("ADempiere.com");
		link.setHref("http://www.adempiere.com");
		link.setTarget("_blank");
		link.setParent(vbox);

		return tabPanel;
	}

	public void onEvent(Event event) throws Exception {
		if (event.getTarget() == bErrorsOnly) {
			this.updateLogTable();
		}
		else if (event.getTarget() == btnDownload)
			downloadLog();
		else if (event.getTarget() == btnViewLog)
			viewLog();
		else if (event.getTarget() == btnErrorEmail)
			cmd_errorEMail();
		else if (event.getTarget() == btnAdempiereLog)
			downloadAdempiereLogFile();
		else if (event.getTarget() == btnServerLog)
			downloadServerLogFile();
		else if (event.getTarget() == levelListBox)
			setTraceLevel();
		else if (event instanceof SizeEvent)
			doResize((SizeEvent)event);
		else if (event instanceof MaximizeEvent)
		{
			MaximizeEvent me = (MaximizeEvent) event;
			if (me.isMaximized())
				doResize(SessionManager.getAppDesktop().getClientInfo().desktopWidth, SessionManager.getAppDesktop().getClientInfo().desktopHeight);
			else
			{
				int width = parseHtmlSizeString(me.getWidth());
				int height = parseHtmlSizeString(me.getHeight());
				doResize(width, height);
			}
		}
		else if (Events.ON_CLICK.equals(event.getName()))
			this.detach();
	}

	private int parseHtmlSizeString(String sizeStr)
	{
		StringBuffer buffer = new StringBuffer();
		for(char ch : sizeStr.toCharArray())
		{
			if (Character.isDigit(ch))
				buffer.append(ch);
			else
				break;
		}
		return Integer.parseInt(buffer.toString());
	}

	private void setTraceLevel() {
		Listitem item = levelListBox.getSelectedItem();
		if (item != null && item.getValue() != null) {
			Level level = (Level) item.getValue();
			CLogMgt.setLevel(level);
			Ini.setProperty(Ini.P_TRACELEVEL, CLogMgt.getLevel().getName());
			Ini.saveProperties(false);
		}
	}

	private void downloadServerLogFile() {
		String path = Ini.getAdempiereHome() + File.separator + "jboss" + File.separator
			+ "server" + File.separator + "adempiere" + File.separator + "log";
		FolderBrowser fileBrowser = new FolderBrowser(path, false);
		String selected = fileBrowser.getPath();
		if (selected != null && selected.trim().length() > 0) {
			File file = new File(selected);
			if (file.exists() && file.isFile() && file.canRead()) {
				try {
					AMedia media = new AMedia(file, "text/plain", null);
					Filedownload.save(media);
				} catch (FileNotFoundException e) {
				}
			}
		}
	}

	private void downloadAdempiereLogFile() {
		String path = Ini.getAdempiereHome() + File.separator + "log";
		FolderBrowser fileBrowser = new FolderBrowser(path, false);
		String selected = fileBrowser.getPath();
		if (selected != null && selected.trim().length() > 0) {
			File file = new File(selected);
			if (file.exists() && file.isFile() && file.canRead()) {
				try {
					AMedia media = new AMedia(file, "text/plain", null);
					Filedownload.save(media);
				} catch (FileNotFoundException e) {
				}
			}
		}
	}

	private void doResize(SizeEvent event) {
		int width = Integer.parseInt(event.getWidth().substring(0, event.getWidth().length() - 2));
		int height = Integer.parseInt(event.getHeight().substring(0, event.getHeight().length() - 2));

		doResize(width, height);
	}

	private void doResize(int width, int height) {
		tabbox.setWidth((width - 20) + "px");
		tabbox.setHeight((height - 70) + "px");

		tabPanels.setWidth((width - 20) + "px");

		logTable.setHeight((height - 160) + "px");
		logTable.setWidth((width - 30) + "px");
	}

	private void downloadLog() {
		String log = CLogErrorBuffer.get(true).getErrorInfo(Env.getCtx(), bErrorsOnly.isChecked());
		AMedia media = new AMedia("trace.log", null, "text/plain", log.getBytes());
		Filedownload.save(media);
	}

	private void viewLog() {
		String log = CLogErrorBuffer.get(true).getErrorInfo(Env.getCtx(), bErrorsOnly.isChecked());
		Window w = new Window();
		w.setAttribute(Window.MODE_KEY, Window.MODE_HIGHLIGHTED);
		w.setTitle("View Log");
		w.setBorder("normal");
		w.setClosable(true);
		w.setMaximizable(true);
		w.setSizable(true);
		w.setWidth("600px");
		w.setHeight("500px");
		Textarea textbox = new Textarea();
		textbox.setDynamicProperty("readonly", "true");
		textbox.setStyle("width:100%; height: 100%");
		w.appendChild(textbox);
		Text text = new Text(log);
		textbox.appendChild(text);
		AEnv.showCenterScreen(w);
	}

	/**
	 * 	EMail Errors
	 */
	private void cmd_errorEMail()
	{
		new WEMailDialog(this,
			"EMail Trace",
			MUser.get(Env.getCtx()),
			"",			//	to
			"Adempiere Trace Info",
			CLogErrorBuffer.get(true).getErrorInfo(Env.getCtx(), bErrorsOnly.isSelected()),
			null);

	}	//	cmd_errorEMail
}
