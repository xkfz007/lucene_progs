package simple_paper_search;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JFrame;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JEditorPane;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.EditorKit;
import javax.swing.JLabel;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.TopDocs;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import java.awt.Font;

public class SimplePaperSearchUI extends JFrame {

	private static final long serialVersionUID = 1L;

	private JPanel jContentPane = null;

	private JPanel jPanel = null;

	private JPanel jPanel1 = null;

	private JComboBox jComboBox = null;

	private JTextField jTextField = null;

	private JButton jButton = null;

	private JButton jButton1 = null;

	private JScrollPane jScrollPane = null;

	private JEditorPane jEditorPane = null;

	private JPanel jPanel2 = null;

	private JLabel jLabel = null;

	private JButton jButton2 = null;
	
	
	HTMLSearch htmlSearcher=new HTMLSearch();
	String queryString="";
	int  indx=0;
	String[] htmlFields={"title","author","workplace","abstract","year"};
	String[]pdfFields={"title","abstract","content"};
	String[]htmlItems={"Title","Author","Workplace","Abstract","YearofPub"};
	String[]pdfItems={"Title","Abstract","Content"};
	TopDocs topDocs=null;
	List<Document> recordList=null;
	int topK=100;
	int pageCount=0;
	int docsEveryPage=20;
	int pageNum=1;
	int firstResult=0;
	int lastResult=docsEveryPage;
	boolean fuzzy=false;

	PDFSearch pdfSearcher=new PDFSearch();
	
	int indexMode=1;//1:HTML,2:PDF;
	
	private JCheckBox jCheckBox = null;

	private JDialog jDialog = null;  //  @jve:decl-index=0:visual-constraint="903,16"

	private JPanel jContentPane1 = null;

	private JButton jButton3 = null;

	private JPanel jPanel3 = null;

	private JPanel jPanel4 = null;

	private JPanel jPanel5 = null;

	private JPanel jPanel6 = null;

	private JPanel jPanel7 = null;

	private JLabel jLabel1 = null;

	private JTextField jTextField1 = null;

	private JLabel jLabel2 = null;

	private JLabel jLabel3 = null;

	private JLabel jLabel4 = null;

	private JLabel jLabel5 = null;

	private JTextField jTextField2 = null;

	private JTextField jTextField3 = null;

	private JTextField jTextField4 = null;

	private JTextField jTextField5 = null;
	
	int htmlFieldNum=htmlFields.length;
	String[] htmlBooleanQueryStr=new String[htmlFieldNum];
	
	float[] htmlWeight=new float[htmlFieldNum];
	
	boolean[] htmlFieldState=new boolean[htmlFieldNum];
	int htmlCheckedNum=0;
	
	int pdfFieldNum=pdfFields.length;
	String[] pdfBooleanQueryStr=new String[pdfFieldNum];
	
	float[] pdfWeight=new float[pdfFieldNum];
	
	boolean[] pdfFieldState=new boolean[pdfFieldNum];
	int pdfCheckedNum=0;
	
	int searchChoose=1;//1:single 2:bool 3:multi
	//public enum fieldEnum{Title,Author,Workplace,Abstract,PublishTime};
	String htmlIndexPath="htmlIndexes";  //  @jve:decl-index=0:
	String pdfIndexPath="pdfIndexes";  //  @jve:decl-index=0:

	private JTabbedPane jTabbedPane = null;

	private JPanel jPanel8 = null;

	private JPanel jPanel9 = null;

	private JTextField jTextField6 = null;

	private JCheckBox jCheckBox1 = null;

	private JCheckBox jCheckBox2 = null;

	private JCheckBox jCheckBox3 = null;

	private JCheckBox jCheckBox4 = null;

	private JCheckBox jCheckBox5 = null;

	private JTextField jTextField7 = null;

	private JPanel jPanel10 = null;

	private JPanel jPanel11 = null;

	private JPanel jPanel12 = null;

	private JPanel jPanel13 = null;

	private JPanel jPanel14 = null;

	private JTextField jTextField8 = null;

	private JTextField jTextField9 = null;

	private JTextField jTextField10 = null;

	private JTextField jTextField11 = null;

	private JButton jButton4 = null;

	private JButton jButton5 = null;

	private JButton jButton6 = null;

	private JComboBox jComboBox1 = null;

	private JDialog jDialog1 = null;  //  @jve:decl-index=0:visual-constraint="907,416"

	private JPanel jContentPane11 = null;

	private JTabbedPane jTabbedPane1 = null;

	private JPanel jPanel81 = null;

	private JPanel jPanel31 = null;

	private JLabel jLabel11 = null;

	private JTextField jTextField13 = null;

	private JPanel jPanel61 = null;

	private JLabel jLabel41 = null;

	private JTextField jTextField41 = null;

	private JPanel jPanel71 = null;

	private JLabel jLabel51 = null;

	private JTextField jTextField51 = null;

	private JButton jButton31 = null;

	private JButton jButton51 = null;

	private JPanel jPanel91 = null;

	private JTextField jTextField61 = null;

	private JPanel jPanel101 = null;

	private JCheckBox jCheckBox11 = null;

	private JTextField jTextField71 = null;

	private JPanel jPanel131 = null;

	private JCheckBox jCheckBox41 = null;

	private JTextField jTextField101 = null;

	private JPanel jPanel141 = null;

	private JCheckBox jCheckBox51 = null;

	private JTextField jTextField111 = null;

	private JButton jButton41 = null;

	private JButton jButton61 = null;

	private JLabel jLabel6 = null;

	/**
	 * This is the default constructor
	 */
	public SimplePaperSearchUI() {
		super();
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setSize(752, 678);
		this.setResizable(false);
		this.setContentPane(getJContentPane());
		this.setTitle("SimplePaperRetrieval");
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setLayout(new BorderLayout());
			jContentPane.add(getJPanel(), BorderLayout.CENTER);
		}
		return jContentPane;
	}

	/**
	 * This method initializes jPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel() {
		if (jPanel == null) {
			jLabel6 = new JLabel();
			jLabel6.setBounds(new Rectangle(15, 598, 697, 31));
			jLabel6.setText("<html><p><strong><font size=\"4\" color=\"blue\">&nbsp;&nbsp;&nbsp;" +
					"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &copy;2010 " +
					"Chinese Academy of Sciences ,made by Hu Fangzhen&Zhang Chunming</font></strong></p></html>");
			jLabel6.setBorder(new EtchedBorder());
			jPanel = new JPanel();
			jPanel.setLayout(null);
			jPanel.add(getJPanel1(), null);
			jPanel.add(getJScrollPane(), null);
			jPanel.add(getJPanel2(), null);
			jPanel.setBorder(new EtchedBorder());
			jPanel.add(jLabel6, null);
		}
		return jPanel;
	}

	/**
	 * This method initializes jPanel1	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel1() {
		if (jPanel1 == null) {
			jPanel1 = new JPanel();
			jPanel1.setLayout(null);
			jPanel1.setBounds(new Rectangle(12, 12, 698, 53));
			jPanel1.add(getJComboBox(), null);
			jPanel1.add(getJTextField(), null);
			jPanel1.add(getJButton(), null);
			jPanel1.add(getJButton2(), null);
			jPanel1.setBorder(new EtchedBorder());
			jPanel1.add(getJCheckBox(), null);
			jPanel1.add(getJComboBox1(), null);
			jPanel1.add(getJButton1(), null);
		}
		return jPanel1;
	}

	/**
	 * This method initializes jComboBox	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox getJComboBox() {
		if (jComboBox == null) {
			jComboBox = new JComboBox();
			jComboBox.setBounds(new Rectangle(80, 7, 83, 36));
			//根据html建的索引进行搜索
			//String[] items={"Title","Author","Workplace","Abstract","YearofPub"};
			for(int i=0;i<htmlItems.length;i++)
			jComboBox.addItem(htmlItems[i]);
		}
		return jComboBox;
	}

	/**
	 * This method initializes jTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJTextField() {
		if (jTextField == null) {
			jTextField = new JTextField();
			jTextField.setBounds(new Rectangle(168, 5, 223, 39));
			jTextField.setFont(new Font("Dialog", Font.BOLD, 14));
			jTextField.addKeyListener(new java.awt.event.KeyAdapter() {
				public void keyPressed(java.awt.event.KeyEvent e) {
					//System.out.println("keyPressed()"); // TODO Auto-generated Event stub keyPressed()
					if(e.getKeyCode() == KeyEvent.VK_ENTER){
						if(indexMode==1)
							htmlSingleFiedlSearch();
						else if(indexMode==2)
						{
							pdfSingleFiedlSearch();
						}
					 }
				}
			});
		}
		return jTextField;
	}

	/**
	 * This method initializes jButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJButton() {
		if (jButton == null) {
			jButton = new JButton();
			jButton.setBounds(new Rectangle(458, 10, 67, 31));
			//ImageIcon image = new ImageIcon("image/logo.jpg"); 
			jButton.setFont(new Font("Dialog", Font.BOLD, 12));
			jButton.setText("<html>Search</html>");
			//jButton.setIcon(image);
			jButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
				//	System.out.println("actionPerformed()"); // TODO Auto-generated Event stub actionPerformed()
					if(indexMode==1)
						htmlSingleFiedlSearch();
					else if(indexMode==2)
					{
						pdfSingleFiedlSearch();
					}
					}
				
			});
			
		}
		return jButton;
	}
	
	/**
	 * This method initializes jButton1	
	 * 	主要的检索方法
	 * @return javax.swing.JButton	
	 */
public void htmlSingleFiedlSearch()
{
	searchChoose=1;
	queryString=jTextField.getText();
	
	if(!queryString.equals(""))
	{
		indx=jComboBox.getSelectedIndex();
		
		String field=htmlFields[indx];
	
		fuzzy=jCheckBox.isSelected();
		topDocs=htmlSearcher.getSingleTopDocs(queryString,field,fuzzy);
	
		long Time = htmlSearcher.time;
		int docCount=htmlSearcher.recordCount;
		
		String newResult="";
		pageCount=0;
		
		//
		if(docCount>0)
		{
		pageCount=(int) Math.ceil((double)docCount/docsEveryPage);
		//设置页号
		pageNum=1;
		//设置指针1
		firstResult=0;
		//设置指针2
		lastResult=docsEveryPage;

		//初始化seacherd的变量
		htmlSearcher.firstResult=firstResult;
		htmlSearcher.lastResult=lastResult;
		//
		//没有使用Termvector的方法
		recordList=htmlSearcher.getHighlightResult(queryString, field, topDocs);
		//使用Termvector后的方法
		//recordList=searcher.getHighlightResultWithTermVector(queryString, field, topDocs);
		
		System.out.println("pageCount="+pageCount);
		//firstResult=0;
	//	lastResult=topK;
		String htmlResult=htmlSearcher.getHtmlResult(recordList);
		
		newResult="<html><body>"+htmlResult+
		"<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"http://up\">上一页</a>" +
		"&nbsp;&nbsp;&nbsp;&nbsp;共"+pageCount+"页&nbsp;&nbsp;,&nbsp;&nbsp;第"
		+pageNum+"页&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"http://next\">下一页</a></body></html>";
		}
		else 
			newResult="<html><body>Sorry,没有搜索到任何结果！！！</body></html>";
//		显示搜索时间和文档数目
		jLabel.setText("<html><body>大约花费<strong><font color=\"blue\">"+Time+
				"</font></strong>毫秒,找到<strong><font color=\"blue\">"+docCount+
				"</font></strong>篇文档，共<strong><font color=\"blue\">"+pageCount+
				"</font></strong>页</body></html>");
		try {
			 //final EditorKit kit = pane.getEditorKitForContentType(type);
			
			jEditorPane.setText(newResult);
			
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		
	//	HyperlinkEvent.EventType.ACTIVATED;
	}
	else
		System.out.println("检索内容不能为空");
}

public void pdfSingleFiedlSearch()
{
	searchChoose=1;
	queryString=jTextField.getText();
	
	if(!queryString.equals(""))
	{
		indx=jComboBox.getSelectedIndex();
		
		String field=pdfFields[indx];
	
		fuzzy=jCheckBox.isSelected();
		topDocs=pdfSearcher.getTopDocs(queryString,field,fuzzy);
	
		long Time = pdfSearcher.time;
		int docCount=pdfSearcher.recordCount;
		
		String newResult="";
		pageCount=0;
		
		//
		if(docCount>0)
		{
		pageCount=(int) Math.ceil((double)docCount/docsEveryPage);
		//设置页号
		pageNum=1;
		//设置指针1
		firstResult=0;
		//设置指针2
		lastResult=docsEveryPage;

		//初始化seacherd的变量
		pdfSearcher.firstResult=firstResult;
		pdfSearcher.lastResult=lastResult;
		//
		//没有使用Termvector的方法
		recordList=pdfSearcher.getHighlightResult(queryString, field, topDocs);
		//使用Termvector后的方法
		//recordList=searcher.getHighlightResultWithTermVector(queryString, field, topDocs);
		
		System.out.println("pageCount="+pageCount);
		//firstResult=0;
	//	lastResult=topK;
		String htmlResult=pdfSearcher.getHtmlResult(recordList);
		
		newResult="<html><body>"+htmlResult+
		"<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"http://up\">上一页</a>" +
		"&nbsp;&nbsp;&nbsp;&nbsp;共"+pageCount+"页&nbsp;&nbsp;,&nbsp;&nbsp;第"
		+pageNum+"页&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"http://next\">下一页</a></body></html>";
		}
		else 
			newResult="<html><body>Sorry,没有搜索到任何结果！！！</body></html>";
//		显示搜索时间和文档数目
		jLabel.setText("<html><body>大约花费<strong><font color=\"blue\">"+Time+
				"</font></strong>毫秒,找到<strong><font color=\"blue\">"+docCount+
				"</font></strong>篇文档，共<strong><font color=\"blue\">"+pageCount+
				"</font></strong>页</body></html>");
		try {
			 //final EditorKit kit = pane.getEditorKitForContentType(type);
			
			jEditorPane.setText(newResult);
			
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		
	//	HyperlinkEvent.EventType.ACTIVATED;
	}
	else
		System.out.println("检索内容不能为空");
}



/**
 * This method initializes jButton1	
 * 	多区域检索
 * @return javax.swing.JButton	
 */
public void htmlBooleanSearch()
{
	searchChoose=2;
	boolean allNull=true;
	htmlBooleanQueryStr[0]=jTextField1.getText();
	htmlBooleanQueryStr[1]=jTextField2.getText();
	htmlBooleanQueryStr[2]=jTextField3.getText();
	htmlBooleanQueryStr[3]=jTextField4.getText();
	htmlBooleanQueryStr[4]=jTextField5.getText();
	for(int i=0;i<htmlFieldNum;i++)
	{
		if(!htmlBooleanQueryStr[i].equalsIgnoreCase(""))
			allNull=false;
	}

	if(!allNull)
	{
		//indx=jComboBox.getSelectedIndex();
		
		//String field=fields[indx];
	
		//fuzzy=jCheckBox.isSelected();
		topDocs=htmlSearcher.getBooleanTopDocs(htmlBooleanQueryStr, htmlFields);
	
		long Time = htmlSearcher.time;
		int docCount=htmlSearcher.recordCount;
		
		String newResult="";
		pageCount=0;
		
		//
		if(docCount>0)
		{
		pageCount=(int) Math.ceil((double)docCount/docsEveryPage);
		//设置页号
		pageNum=1;
		//设置指针1
		firstResult=0;
		//设置指针2
		lastResult=docsEveryPage;

		//初始化seacherd的变量
		htmlSearcher.firstResult=firstResult;
		htmlSearcher.lastResult=lastResult;
		//
		//没有使用Termvector的方法
		recordList=htmlSearcher.getBooleanHighlightResult(htmlBooleanQueryStr, htmlFields, topDocs);
		//使用Termvector后的方法
		//recordList=searcher.getHighlightResultWithTermVector(queryString, field, topDocs);
		
		System.out.println("pageCount="+pageCount);
		//firstResult=0;
	//	lastResult=topK;
		String htmlResult=htmlSearcher.getHtmlResult(recordList);
		
		newResult="<html><body>"+htmlResult+
		"<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"http://up\">上一页</a>" +
		"&nbsp;&nbsp;&nbsp;&nbsp;共"+pageCount+"页&nbsp;&nbsp;,&nbsp;&nbsp;第"
		+pageNum+"页&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"http://next\">下一页</a></body></html>";
		}
		else 
			newResult="<html><body>Sorry,没有搜索到任何结果！！！</body></html>";
//		显示搜索时间和文档数目
		jLabel.setText("<html><body>大约花费<strong><font color=\"blue\">"+Time+
				"</font></strong>毫秒,找到<strong><font color=\"blue\">"+docCount+
				"</font></strong>篇文档，共<strong><font color=\"blue\">"+pageCount+
				"</font></strong>页</body></html>");
		try {
			 //final EditorKit kit = pane.getEditorKitForContentType(type);
			
			jEditorPane.setText(newResult);
			
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		
	//	HyperlinkEvent.EventType.ACTIVATED;
	}
	else
		System.out.println("检索内容不能为空");
}


/**
 * This method initializes jButton1	
 * 	多区域检索
 * @return javax.swing.JButton	
 */
public void pdfBooleanSearch()
{
	searchChoose=2;
	boolean allNull=true;
	pdfBooleanQueryStr[0]=jTextField13.getText();
	pdfBooleanQueryStr[1]=jTextField41.getText();
	pdfBooleanQueryStr[2]=jTextField51.getText();

	for(int i=0;i<pdfFieldNum;i++)
	{
		if(!pdfBooleanQueryStr[i].equalsIgnoreCase(""))
			allNull=false;
	}

	if(!allNull)
	{

		topDocs=pdfSearcher.getBooleanTopDocs(pdfBooleanQueryStr, pdfFields);
	
		long Time = pdfSearcher.time;
		int docCount=pdfSearcher.recordCount;
		
		String newResult="";
		pageCount=0;
		
		//
		if(docCount>0)
		{
		pageCount=(int) Math.ceil((double)docCount/docsEveryPage);
		//设置页号
		pageNum=1;
		//设置指针1
		firstResult=0;
		//设置指针2
		lastResult=docsEveryPage;

		//初始化seacherd的变量
		pdfSearcher.firstResult=firstResult;
		pdfSearcher.lastResult=lastResult;
		//
		//没有使用Termvector的方法
		recordList=pdfSearcher.getBooleanHighlightResult(pdfBooleanQueryStr, pdfFields, topDocs);
		//使用Termvector后的方法
		//recordList=searcher.getHighlightResultWithTermVector(queryString, field, topDocs);
		
		System.out.println("pageCount="+pageCount);
		//firstResult=0;
	//	lastResult=topK;
		String htmlResult=pdfSearcher.getHtmlResult(recordList);
		
		System.out.println(htmlResult);
		newResult="<html><body>"+htmlResult+
		"<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"http://up\">上一页</a>" +
		"&nbsp;&nbsp;&nbsp;&nbsp;共"+pageCount+"页&nbsp;&nbsp;,&nbsp;&nbsp;第"
		+pageNum+"页&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"http://next\">下一页</a></body></html>";
		}
		else 
			newResult="<html><body>Sorry,we can find nothing！！！</body></html>";
//		显示搜索时间和文档数目
		jLabel.setText("<html><body>大约花费<strong><font color=\"blue\">"+Time+
				"</font></strong>毫秒,找到<strong><font color=\"blue\">"+docCount+
				"</font></strong>篇文档，共<strong><font color=\"blue\">"+pageCount+
				"</font></strong>页</body></html>");
		try {
			 //final EditorKit kit = pane.getEditorKitForContentType(type);
			
			jEditorPane.setText(newResult);
			
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		
	//	HyperlinkEvent.EventType.ACTIVATED;
	}
	else
		System.out.println("检索内容不能为空");
}


///==========
public void htmlMultiFiedlSearch()
{
	searchChoose=3;
	queryString=jTextField6.getText();
	htmlWeight[0]=Float.parseFloat(getJTextField7().getText());
	htmlWeight[1]=Float.parseFloat(getJTextField8().getText());
	htmlWeight[2]=Float.parseFloat(getJTextField9().getText());
	htmlWeight[3]=Float.parseFloat(getJTextField10().getText());
	htmlWeight[4]=Float.parseFloat(getJTextField11().getText());
	if(!queryString.equals("")&&htmlCheckedNum>0)
	{
		
		
		//indx=jComboBox.getSelectedIndex();
			 
		String[] tmpField=new String[htmlCheckedNum];
		float[]tmpWeight=new  float[htmlCheckedNum];
		for(int i=0,j=0;i<htmlFieldNum;i++)
		{
			System.out.println("i="+i+"   j="+j);
			if(htmlFieldState[i])
			{
				tmpField[j]=htmlFields[i];
				tmpWeight[j]=htmlWeight[i];
				j++;
			}
		}
		
		//fuzzy=jCheckBox.isSelected();
		topDocs=htmlSearcher.getMultiFieldTopDocs(queryString, tmpField, tmpWeight);
	
		long Time = htmlSearcher.time;
		int docCount=htmlSearcher.recordCount;
		
		String newResult="";
		pageCount=0;
		
		//
		if(docCount>0)
		{
		pageCount=(int) Math.ceil((double)docCount/docsEveryPage);
		//设置页号
		pageNum=1;
		//设置指针1
		firstResult=0;
		//设置指针2
		lastResult=docsEveryPage;

		//初始化seacherd的变量
		htmlSearcher.firstResult=firstResult;
		htmlSearcher.lastResult=lastResult;
		//
		//没有使用Termvector的方法
		recordList=htmlSearcher.getMultiFieldHighlightResult(queryString, tmpField, tmpWeight,topDocs);
		//使用Termvector后的方法
		//recordList=searcher.getHighlightResultWithTermVector(queryString, field, topDocs);
		
		System.out.println("pageCount="+pageCount);
		//firstResult=0;
	//	lastResult=topK;
		String htmlResult=htmlSearcher.getHtmlResult(recordList);
		
		newResult="<html><body>"+htmlResult+
		"<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"http://up\">上一页</a>" +
		"&nbsp;&nbsp;&nbsp;&nbsp;共"+pageCount+"页&nbsp;&nbsp;,&nbsp;&nbsp;第"
		+pageNum+"页&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"http://next\">下一页</a></body></html>";
		}
		else 
			newResult="<html><body>Sorry,没有搜索到任何结果！！！</body></html>";
//		显示搜索时间和文档数目
		jLabel.setText("<html><body>大约花费<strong><font color=\"blue\">"+Time+
				"</font></strong>毫秒,找到<strong><font color=\"blue\">"+docCount+
				"</font></strong>篇文档，共<strong><font color=\"blue\">"+pageCount+
				"</font></strong>页</body></html>");
		try {
			 //final EditorKit kit = pane.getEditorKitForContentType(type);
			
			jEditorPane.setText(newResult);
			
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		
	//	HyperlinkEvent.EventType.ACTIVATED;
	}
	else
		System.out.println("检索内容不能为空");
}

///==========
public void pdfMultiFiedlSearch()
{
	searchChoose=3;
	queryString=jTextField61.getText();
	pdfWeight[0]=Float.parseFloat(getJTextField71().getText());
	pdfWeight[1]=Float.parseFloat(getJTextField101().getText());
	pdfWeight[2]=Float.parseFloat(getJTextField111().getText());
	
	if(!queryString.equals("")&&pdfCheckedNum>0)
	{
		
		
		//indx=jComboBox.getSelectedIndex();
			 
		String[] tmpField=new String[pdfCheckedNum];
		float[]tmpWeight=new  float[pdfCheckedNum];
		for(int i=0,j=0;i<pdfFieldNum;i++)
		{
			System.out.println("pdf i="+i+"   j="+j);
			if(pdfFieldState[i])
			{
				tmpField[j]=pdfFields[i];
				tmpWeight[j]=pdfWeight[i];
				j++;
			}
		}
		
		//fuzzy=jCheckBox.isSelected();
		topDocs=pdfSearcher.getMultiFieldTopDocs(queryString, tmpField, tmpWeight);
	
		long Time = pdfSearcher.time;
		int docCount=pdfSearcher.recordCount;
		
		String newResult="";
		pageCount=0;
		
		//
		if(docCount>0)
		{
		pageCount=(int) Math.ceil((double)docCount/docsEveryPage);
		//设置页号
		pageNum=1;
		//设置指针1
		firstResult=0;
		//设置指针2
		lastResult=docsEveryPage;

		//初始化seacherd的变量
		pdfSearcher.firstResult=firstResult;
		pdfSearcher.lastResult=lastResult;
		//
		//没有使用Termvector的方法
		recordList=pdfSearcher.getMultiFieldHighlightResult(queryString, tmpField, tmpWeight,topDocs);
		//使用Termvector后的方法
		//recordList=searcher.getHighlightResultWithTermVector(queryString, field, topDocs);
		
		System.out.println("pageCount="+pageCount);
		//firstResult=0;
	//	lastResult=topK;
		String htmlResult=pdfSearcher.getHtmlResult(recordList);
		
		newResult="<html><body>"+htmlResult+
		"<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"http://up\">上一页</a>" +
		"&nbsp;&nbsp;&nbsp;&nbsp;共"+pageCount+"页&nbsp;&nbsp;,&nbsp;&nbsp;第"
		+pageNum+"页&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"http://next\">下一页</a></body></html>";
		}
		else 
			newResult="<html><body>Sorry,没有搜索到任何结果！！！</body></html>";
//		显示搜索时间和文档数目
		jLabel.setText("<html><body>大约花费<strong><font color=\"blue\">"+Time+
				"</font></strong>毫秒,找到<strong><font color=\"blue\">"+docCount+
				"</font></strong>篇文档，共<strong><font color=\"blue\">"+pageCount+
				"</font></strong>页</body></html>");
		try {
			 //final EditorKit kit = pane.getEditorKitForContentType(type);
			
			jEditorPane.setText(newResult);
			
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		
	//	HyperlinkEvent.EventType.ACTIVATED;
	}
	else
		System.out.println("检索内容不能为空");
}


	/**
	 * This method initializes jButton1	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJButton1() {
		if (jButton1 == null) {
			jButton1 = new JButton();
			jButton1.setText("<html>Index</html>");
			jButton1.setBounds(new Rectangle(9, 7, 63, 36));
			jButton1.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					int ans=JOptionPane.showConfirmDialog(null, "Do you really want to index the files?", "Index", JOptionPane.YES_NO_OPTION);
					if(ans==JOptionPane.YES_OPTION)
					{
						String indexProcess="";
						if(indexMode==1)
						{
							delAllFile(htmlIndexPath);
							HTMLIndex htmlIndex=new HTMLIndex();
							indexProcess=htmlIndex.createHtmlIndex();
						}
						else if(indexMode==2)
						{
							delAllFile(pdfIndexPath);
							PDFIndex pdfIndex=new PDFIndex();
							indexProcess=pdfIndex.createPdfIndex();
						}
						jEditorPane.setText("<html><body>"+indexProcess+"</body></html>");
						
					}
				}
			});
		}
		return jButton1;
	}
    /**
     * 指定删除目录路径构造一个文件对象

     */
	public void delAllFile(String filePath)
	   {

	       File file = new File(filePath);
	      
	       File[] fileList = file.listFiles();

	       String dirPath = null;
	      
	       if(fileList != null)
	           for(int i = 0 ; i < fileList.length; i++)
	           {
	               /**
	                * 如果是文件就将其删除
	                */
	               if(fileList[i].isFile())
	                   fileList[i].delete();
	               /**
	                * 如果是目录,那么将些目录下所有文件删除后再将其目录删除,
	                */
	               if(fileList[i].isDirectory()){
	                   dirPath = fileList[i].getPath();
	                   //递归删除指定目录下所有文件
	                   delAllFile(dirPath);
	               }
	           }
	}
	/**
	 * This method initializes jScrollPane	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getJScrollPane() {
		if (jScrollPane == null) {
			jScrollPane = new JScrollPane();
			jScrollPane.setBounds(new Rectangle(14, 125, 698, 467));
			jScrollPane.setViewportView(getJEditorPane());
			jScrollPane.setBorder(new EtchedBorder());
		}
		return jScrollPane;
	}

	/**
	 * This method initializes jEditorPane	
	 * 	
	 * @return javax.swing.JEditorPane	
	 */
	private JEditorPane getJEditorPane() {
		if (jEditorPane == null) {
			jEditorPane = new JEditorPane();
			jEditorPane.setEditable(false);
			jEditorPane.setFont(new Font("Dialog", Font.PLAIN, 13));
			final EditorKit kit = jEditorPane.getEditorKitForContentType("text/html");
			jEditorPane.setEditorKit(kit);
			jEditorPane.addHyperlinkListener(new HyperlinkListener() {
		        public void hyperlinkUpdate(HyperlinkEvent e) {
		          if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) 
		          {
		            try {
		            	if(e.getURL()!=null)
		            	{
		            	if(e.getURL().toString().equalsIgnoreCase("http://up"))
		            	{
		            		System.out.println(e.getURL().toString()+"    pageNum="+pageNum+"  searchChoose="+searchChoose);
		          		            		
		            		if(pageNum>1)
		            		{
		            			
		            			pageNum--;
		            			
		            			firstResult=firstResult-docsEveryPage;
		            			lastResult=lastResult-docsEveryPage;
		            		
                     			//
		            			String showHTMLResult="";
		            			if(indexMode==1)
		            			{
		            				htmlSearcher.firstResult=firstResult;
			            			htmlSearcher.lastResult=lastResult;
		            				if(searchChoose==1)
			            			{
			            			String field=htmlFields[indx];
			            			recordList=htmlSearcher.getHighlightResult(queryString, field, topDocs);
			            			}
			            			else if(searchChoose==2)
			            			{
			            				recordList=htmlSearcher.getBooleanHighlightResult(htmlBooleanQueryStr, htmlFields, topDocs);
			            			}
			            			else if(searchChoose==3)
			            			{
			            				String[] tmpField=new String[htmlCheckedNum];
			            				float[]tmpWeight=new  float[htmlCheckedNum];
			            				for(int i=0,j=0;i<htmlFieldNum;i++)
			            				{
			            					//System.out.println("i="+i+"   j="+j);
			            					if(htmlFieldState[i])
			            					{
			            						tmpField[j]=htmlFields[i];
			            						tmpWeight[j]=htmlWeight[i];
			            						j++;
			            					}
			            				}
			            				
			            				recordList=htmlSearcher.getMultiFieldHighlightResult(queryString, tmpField, tmpWeight,topDocs);
			            			}
		            				showHTMLResult=htmlSearcher.getHtmlResult(recordList);
		            			}
		            			else if(indexMode==2)
		            			{
		            				pdfSearcher.firstResult=firstResult;
			            			pdfSearcher.lastResult=lastResult;
		            				if(searchChoose==1)
			            			{
			            			String field=pdfFields[indx];
			            			recordList=pdfSearcher.getHighlightResult(queryString, field, topDocs);
			            			}
			            			else if(searchChoose==2)
			            			{
			            				recordList=pdfSearcher.getBooleanHighlightResult(pdfBooleanQueryStr, pdfFields, topDocs);
			            			}
			            			else if(searchChoose==3)
			            			{
			            				String[] tmpField=new String[pdfCheckedNum];
			            				float[]tmpWeight=new  float[pdfCheckedNum];
			            				for(int i=0,j=0;i<pdfFieldNum;i++)
			            				{
			            					//System.out.println("i="+i+"   j="+j);
			            					if(pdfFieldState[i])
			            					{
			            						tmpField[j]=pdfFields[i];
			            						tmpWeight[j]=pdfWeight[i];
			            						j++;
			            					}
			            				}
			            				
			            				recordList=pdfSearcher.getMultiFieldHighlightResult(queryString, tmpField, tmpWeight,topDocs);
			            			}
		            				showHTMLResult=pdfSearcher.getHtmlResult(recordList);
		            				
		            			}
		            			
		      
		            			//使用Termvector后的方法
		            			//recordList=searcher.getHighlightResultWithTermVector(queryString, field, topDocs);
		            			//firstResult=0;
		            		//	lastResult=topK;
		            			
		            			
		            			try {
		            				 //final EditorKit kit = pane.getEditorKitForContentType(type);
		            				String newResult="<html><body>"+showHTMLResult+
		            				"<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"http://up\">上一页</a>" +
		            				"&nbsp;&nbsp;&nbsp;&nbsp;共<strong><font color=\"blue\">"+pageCount+
		            					"</font></strong>页&nbsp;&nbsp;,&nbsp;&nbsp;第"
		            				+pageNum+"页&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"http://next\">下一页</a></body></html>";
		            				jEditorPane.setText(newResult);
		            				
		            			} catch (Exception e1) {
		            				// TODO Auto-generated catch block
		            				e1.printStackTrace();
		            			}
		            		}
		            	}
		            	else if(e.getURL().toString().equalsIgnoreCase("http://next"))
		            	{
		            		System.out.println(e.getURL().toString()+"    pageNum="+pageNum+"  searchChoose="+searchChoose);
		            		
		            		if(pageNum<pageCount)
		            		{
		            			pageNum++;
		            			
		            			firstResult=firstResult+docsEveryPage;
		            			lastResult=lastResult+docsEveryPage;
		            			
		            			String showHTMLResult="";
		            			if(indexMode==1)
		            			{
		            				htmlSearcher.firstResult=firstResult;
			            			htmlSearcher.lastResult=lastResult;
		            				if(searchChoose==1)
			            			{
			            			String field=htmlFields[indx];
			            			recordList=htmlSearcher.getHighlightResult(queryString, field, topDocs);
			            			}
			            			else if(searchChoose==2)
			            			{
			            				recordList=htmlSearcher.getBooleanHighlightResult(htmlBooleanQueryStr, htmlFields, topDocs);
			            			}
			            			else if(searchChoose==3)
			            			{
			            				String[] tmpField=new String[htmlCheckedNum];
			            				float[]tmpWeight=new  float[htmlCheckedNum];
			            				for(int i=0,j=0;i<htmlFieldNum;i++)
			            				{
			            					//System.out.println("i="+i+"   j="+j);
			            					if(htmlFieldState[i])
			            					{
			            						tmpField[j]=htmlFields[i];
			            						tmpWeight[j]=htmlWeight[i];
			            						j++;
			            					}
			            				}
			            				
			            				recordList=htmlSearcher.getMultiFieldHighlightResult(queryString, tmpField, tmpWeight,topDocs);
			            			}
		            				showHTMLResult=htmlSearcher.getHtmlResult(recordList);
		            			}
		            			else if(indexMode==2)
		            			{
		            				pdfSearcher.firstResult=firstResult;
			            			pdfSearcher.lastResult=lastResult;
		            				if(searchChoose==1)
			            			{
			            			String field=pdfFields[indx];
			            			recordList=pdfSearcher.getHighlightResult(queryString, field, topDocs);
			            			}
			            			else if(searchChoose==2)
			            			{
			            				recordList=pdfSearcher.getBooleanHighlightResult(pdfBooleanQueryStr, pdfFields, topDocs);
			            			}
			            			else if(searchChoose==3)
			            			{
			            				String[] tmpField=new String[pdfCheckedNum];
			            				float[]tmpWeight=new  float[pdfCheckedNum];
			            				for(int i=0,j=0;i<pdfFieldNum;i++)
			            				{
			            					//System.out.println("i="+i+"   j="+j);
			            					if(pdfFieldState[i])
			            					{
			            						tmpField[j]=pdfFields[i];
			            						tmpWeight[j]=pdfWeight[i];
			            						j++;
			            					}
			            				}
			            				
			            				recordList=pdfSearcher.getMultiFieldHighlightResult(queryString, tmpField, tmpWeight,topDocs);
			            			}
		            				showHTMLResult=pdfSearcher.getHtmlResult(recordList);
		            				
		            			}
		            			
		            			try {
		            				 //final EditorKit kit = pane.getEditorKitForContentType(type);
		            				String newResult="<html><body>"+showHTMLResult+
		            				"<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"http://up\">上一页</a>" +
		            				"&nbsp;&nbsp;&nbsp;&nbsp;共<strong><font color=\"blue\">"+pageCount+
		            					"</font></strong>页&nbsp;&nbsp;,&nbsp;&nbsp;第"
		            				+pageNum+"页&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"http://next\">下一页</a></body></html>";
		            				jEditorPane.setText(newResult);
		            				
		            			} catch (Exception e1) {
		            				// TODO Auto-generated catch block
		            				e1.printStackTrace();
		            			}
		            		}
		            		
		            	}
		            	}
		            	
		            }
		            catch (Exception ex) {
		              ex.printStackTrace();
		            }
		          }
		        }
		      });			
		}
		return jEditorPane;
	}
	/**
	 * This method initializes jPanel2	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel2() {
		if (jPanel2 == null) {
			jLabel = new JLabel();
			jLabel.setText("");
			jLabel.setBorder(new  EtchedBorder());
			jPanel2 = new JPanel();
			jPanel2.setLayout(new BorderLayout());
			jPanel2.setBounds(new Rectangle(13, 68, 698, 53));
			jPanel2.setBorder(new  EtchedBorder());
			jPanel2.add(jLabel, BorderLayout.CENTER);
		}
		return jPanel2;
	}

	/**
	 * This method initializes jButton2	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJButton2() {
		if (jButton2 == null) {
			jButton2 = new JButton();
			jButton2.setBounds(new Rectangle(531, 9, 83, 33));
			jButton2.setText("<html>Advanced</html>");
			jButton2.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					//System.out.println("actionPerformed()"); // TODO Auto-generated Event stub actionPerformed()
				if(jComboBox1.getSelectedIndex()==0)
					getJDialog().show();
				else if(jComboBox1.getSelectedIndex()==1)
					getJDialog1().show();
				}
			});
		}
		return jButton2;
	}



	/**
	 * This method initializes jCheckBox	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getJCheckBox() {
		if (jCheckBox == null) {
			jCheckBox = new JCheckBox();
			jCheckBox.setBounds(new Rectangle(395, 10, 55, 31));
			jCheckBox.setText("<html>Fuzzy</html>");
			jCheckBox.setBorder(new EtchedBorder());
			
		}
		return jCheckBox;
	}

	/**
	 * This method initializes jDialog	
	 * 	
	 * @return javax.swing.JDialog	
	 */
	private JDialog getJDialog() {
		if (jDialog == null) {
			jDialog = new JDialog(this);
			jDialog.setSize(new Dimension(355, 374));
			jDialog.setTitle("HTMLAdvanced");
			jDialog.setResizable(false);
			jDialog.setContentPane(getJContentPane1());
			jDialog.setModal(true);
		}
		return jDialog;
	}

	/**
	 * This method initializes jContentPane1	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJContentPane1() {
		if (jContentPane1 == null) {
			jContentPane1 = new JPanel();
			jContentPane1.setLayout(new BorderLayout());
			jContentPane1.add(getJTabbedPane(), BorderLayout.CENTER);
		}
		return jContentPane1;
	}

	/**
	 * This method initializes jButton3	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJButton3() {
		if (jButton3 == null) {
			jButton3 = new JButton();
			jButton3.setText("<html>Search</html>");
			jButton3.setBounds(new Rectangle(81, 269, 80, 31));
			jButton3.setHorizontalAlignment(SwingConstants.CENTER);
			jButton3.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					//System.out.println("actionPerformed()"); // TODO Auto-generated Event stub actionPerformed()
					htmlBooleanSearch();
					jDialog.dispose();

				}
			});
		}
		return jButton3;
	}

	/**
	 * This method initializes jPanel3	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel3() {
		if (jPanel3 == null) {
			jLabel1 = new JLabel();
			jLabel1.setBounds(new Rectangle(4, 5, 95, 32));
			jLabel1.setText("<html><strong>TITLE</strong></html>");
			jLabel1.setHorizontalAlignment(JLabel.CENTER);
			jLabel1.setBorder(new EtchedBorder());
			jPanel3 = new JPanel();
			jPanel3.setLayout(null);
			jPanel3.setBounds(new Rectangle(14, 5, 292, 43));
			jPanel3.add(jLabel1, null);
			jPanel3.add(getJTextField1(), null);
			jPanel3.setBorder(new EtchedBorder());
		}
		return jPanel3;
	}

	/**
	 * This method initializes jPanel4	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel4() {
		if (jPanel4 == null) {
			jLabel2 = new JLabel();
			jLabel2.setBounds(new Rectangle(7, 5, 94, 35));
			jLabel2.setText("<html>AUTHOR</html>");
			jLabel2.setHorizontalAlignment(SwingConstants.CENTER);
			jLabel2.setBorder(new EtchedBorder());
			jPanel4 = new JPanel();
			jPanel4.setLayout(null);
			jPanel4.setBounds(new Rectangle(13, 50, 292, 44));
			jPanel4.add(jLabel2, null);
			jPanel4.add(getJTextField2(), null);
			jPanel4.setBorder(new EtchedBorder());
		}
		return jPanel4;
	}

	/**
	 * This method initializes jPanel5	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel5() {
		if (jPanel5 == null) {
			jLabel3 = new JLabel();
			jLabel3.setBounds(new Rectangle(10, 6, 91, 33));
			jLabel3.setText("<html>WORKPLACE</html>");
			jLabel3.setHorizontalAlignment(SwingConstants.CENTER);
			jLabel3.setBorder(new EtchedBorder());
			jPanel5 = new JPanel();
			jPanel5.setLayout(null);
			jPanel5.setBounds(new Rectangle(13, 100, 292, 51));
			jPanel5.add(jLabel3, null);
			jPanel5.add(getJTextField3(), null);
			jPanel5.setBorder(new EtchedBorder());
		}
		return jPanel5;
	}

	/**
	 * This method initializes jPanel6	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel6() {
		if (jPanel6 == null) {
			jLabel4 = new JLabel();
			jLabel4.setBounds(new Rectangle(10, 6, 92, 36));
			jLabel4.setText("<html>ABSTRACT</html>");
			jLabel4.setHorizontalAlignment(SwingConstants.CENTER);
			jLabel4.setBorder(new EtchedBorder());
			jPanel6 = new JPanel();
			jPanel6.setLayout(null);
			jPanel6.setBounds(new Rectangle(12, 158, 293, 50));
			jPanel6.add(jLabel4, null);
			jPanel6.add(getJTextField4(), null);
			jPanel6.setBorder(new EtchedBorder());
		}
		return jPanel6;
	}

	/**
	 * This method initializes jPanel7	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel7() {
		if (jPanel7 == null) {
			jLabel5 = new JLabel();
			jLabel5.setBounds(new Rectangle(8, 5, 95, 36));
			jLabel5.setText("<html>PUBLISHTIME</html>");
			jLabel5.setHorizontalAlignment(SwingConstants.CENTER);
			jLabel5.setBorder(new EtchedBorder());
			jPanel7 = new JPanel();
			jPanel7.setLayout(null);
			jPanel7.setBounds(new Rectangle(12, 215, 293, 49));
			jPanel7.add(jLabel5, null);
			jPanel7.add(getJTextField5(), null);
			jPanel7.setBorder(new EtchedBorder());
		}
		return jPanel7;
	}

	/**
	 * This method initializes jTextField1	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJTextField1() {
		if (jTextField1 == null) {
			jTextField1 = new JTextField();
			jTextField1.setBounds(new Rectangle(104, 4, 177, 33));
			jTextField1.setFont(new Font("Dialog", Font.BOLD, 14));
		}
		return jTextField1;
	}

	/**
	 * This method initializes jTextField2	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJTextField2() {
		if (jTextField2 == null) {
			jTextField2 = new JTextField();
			jTextField2.setBounds(new Rectangle(108, 6, 173, 32));
			jTextField2.setFont(new Font("Dialog", Font.BOLD, 14));
		}
		return jTextField2;
	}

	/**
	 * This method initializes jTextField3	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJTextField3() {
		if (jTextField3 == null) {
			jTextField3 = new JTextField();
			jTextField3.setBounds(new Rectangle(109, 9, 172, 31));
			jTextField3.setFont(new Font("Dialog", Font.BOLD, 14));
		}
		return jTextField3;
	}

	/**
	 * This method initializes jTextField4	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJTextField4() {
		if (jTextField4 == null) {
			jTextField4 = new JTextField();
			jTextField4.setBounds(new Rectangle(111, 7, 169, 37));
			jTextField4.setFont(new Font("Dialog", Font.BOLD, 14));
		}
		return jTextField4;
	}

	/**
	 * This method initializes jTextField5	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJTextField5() {
		if (jTextField5 == null) {
			jTextField5 = new JTextField();
			jTextField5.setBounds(new Rectangle(113, 6, 166, 36));
			jTextField5.setFont(new Font("Dialog", Font.BOLD, 14));
		}
		return jTextField5;
	}

	/**
	 * This method initializes jTabbedPane	
	 * 	
	 * @return javax.swing.JTabbedPane	
	 */
	private JTabbedPane getJTabbedPane() {
		if (jTabbedPane == null) {
			jTabbedPane = new JTabbedPane();
			//jTabbedPane.setName("BooleanSearch");
			jTabbedPane.addTab("BooleanSearch", null, getJPanel8(), null);
			jTabbedPane.addTab("MultiFieldSearch", null, getJPanel9(), null);
		}
		return jTabbedPane;
	}

	/**
	 * This method initializes jPanel8	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel8() {
		if (jPanel8 == null) {
			jPanel8 = new JPanel();
			jPanel8.setLayout(null);
			jPanel8.add(getJPanel3(), null);
			jPanel8.add(getJPanel4(), null);
			jPanel8.add(getJPanel5(), null);
			jPanel8.add(getJPanel6(), null);
			jPanel8.add(getJPanel7(), null);
			jPanel8.add(getJButton3(), null);
			jPanel8.add(getJButton5(), null);
		}
		return jPanel8;
	}

	/**
	 * This method initializes jPanel9	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel9() {
		if (jPanel9 == null) {
			jPanel9 = new JPanel();
			jPanel9.setLayout(null);
			jPanel9.add(getJTextField6(), null);
			jPanel9.add(getJPanel10(), null);
			jPanel9.add(getJPanel11(), null);
			jPanel9.add(getJPanel12(), null);
			jPanel9.add(getJPanel13(), null);
			jPanel9.add(getJPanel14(), null);
			jPanel9.add(getJButton4(), null);
			jPanel9.add(getJButton6(), null);
		}
		return jPanel9;
	}

	/**
	 * This method initializes jTextField6	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJTextField6() {
		if (jTextField6 == null) {
			jTextField6 = new JTextField();
			jTextField6.setBounds(new Rectangle(25, 6, 265, 45));
			jTextField6.setFont(new Font("Dialog", Font.BOLD, 15));
		}
		return jTextField6;
	}

	/**
	 * This method initializes jCheckBox1	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getJCheckBox1() {
		if (jCheckBox1 == null) {
			jCheckBox1 = new JCheckBox();
			jCheckBox1.setText("Title");
			jCheckBox1.setBounds(new Rectangle(11, 6, 62, 25));
			//jCheckBox1.setBorder(new EtchedBorder());
			jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					if(jCheckBox1.isSelected())
					{
						htmlFieldState[0]=true;
						htmlCheckedNum++;
						getJTextField7().setEditable(true);
						getJTextField7().setText("1.0");
						//weight[0]
					}
					else
					{
						htmlFieldState[0]=false;
						htmlCheckedNum--;
						getJTextField7().setEditable(false);
						getJTextField7().setText("1.0");
					}
				}
			});
			
			
		}
		return jCheckBox1;
	}

	/**
	 * This method initializes jCheckBox2	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getJCheckBox2() {
		if (jCheckBox2 == null) {
			jCheckBox2 = new JCheckBox();
			jCheckBox2.setText("Author");
			jCheckBox2.setBounds(new Rectangle(8, 3, 66, 25));
			jCheckBox2.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					if(jCheckBox2.isSelected())
					{
						htmlFieldState[1]=true;
						htmlCheckedNum++;
						getJTextField8().setEditable(true);
						getJTextField8().setText("1.0");
						//weight[0]
					}
					else
					{
						htmlFieldState[1]=false;
						htmlCheckedNum--;
						getJTextField8().setEditable(false);
						getJTextField8().setText("1.0");
					}
				}
			});
		}
		return jCheckBox2;
	}

	/**
	 * This method initializes jCheckBox3	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getJCheckBox3() {
		if (jCheckBox3 == null) {
			jCheckBox3 = new JCheckBox();
			jCheckBox3.setText("Workplace");
			jCheckBox3.setBounds(new Rectangle(6, 4, 90, 27));
			jCheckBox3.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					if(jCheckBox3.isSelected())
					{
						htmlFieldState[2]=true;
						htmlCheckedNum++;
						getJTextField9().setEditable(true);
						getJTextField9().setText("1.0");
						//weight[0]
					}
					else
					{
						htmlFieldState[2]=false;
						htmlCheckedNum--;
						getJTextField9().setEditable(false);
						getJTextField9().setText("1.0");
					}
				}
			});
		
		}
		return jCheckBox3;
	}

	/**
	 * This method initializes jCheckBox4	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getJCheckBox4() {
		if (jCheckBox4 == null) {
			jCheckBox4 = new JCheckBox();
			jCheckBox4.setText("Abstract");
			jCheckBox4.setBounds(new Rectangle(5, 6, 86, 24));
			jCheckBox4.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					if(jCheckBox4.isSelected())
					{
						htmlFieldState[3]=true;
						htmlCheckedNum++;
						getJTextField10().setEditable(true);
						getJTextField10().setText("1.0");
						//weight[0]
					}
					else
					{
						htmlFieldState[3]=false;
						htmlCheckedNum--;
						getJTextField10().setEditable(false);
						getJTextField10().setText("1.0");
					}
				}
			});
		}
		return jCheckBox4;
	}

	/**
	 * This method initializes jCheckBox5	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getJCheckBox5() {
		if (jCheckBox5 == null) {
			jCheckBox5 = new JCheckBox();
			jCheckBox5.setText("PubTime");
			jCheckBox5.setBounds(new Rectangle(4, 4, 89, 27));
			jCheckBox5.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					if(jCheckBox5.isSelected())
					{
						htmlFieldState[4]=true;
						htmlCheckedNum++;
						getJTextField11().setEditable(true);
						getJTextField11().setText("1.0");
						//weight[0]
					}
					else
					{
						htmlFieldState[4]=false;
						htmlCheckedNum--;
						getJTextField11().setEditable(false);
						getJTextField11().setText("1.0");
					}
				}
			});
		}
		return jCheckBox5;
	}

	/**
	 * This method initializes jTextField7	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJTextField7() {
		if (jTextField7 == null) {
			jTextField7 = new JTextField();
			jTextField7.setBounds(new Rectangle(97, 5, 79, 25));
			jTextField7.setEditable(false);
			jTextField7.setText("1.0");
		}
		return jTextField7;
	}

	/**
	 * This method initializes jPanel10	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel10() {
		if (jPanel10 == null) {
			jPanel10 = new JPanel();
			jPanel10.setLayout(null);
			jPanel10.setBounds(new Rectangle(25, 55, 265, 37));
			jPanel10.setBorder(new EtchedBorder());
			jPanel10.add(getJCheckBox1(), null);
			jPanel10.add(getJTextField7(), null);
		}
		return jPanel10;
	}

	/**
	 * This method initializes jPanel11	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel11() {
		if (jPanel11 == null) {
			jPanel11 = new JPanel();
			jPanel11.setLayout(null);
			jPanel11.setBounds(new Rectangle(25, 94, 265, 37));
			jPanel11.setBorder(new EtchedBorder());
			jPanel11.add(getJCheckBox2(), null);
			jPanel11.add(getJTextField8(), null);
		}
		return jPanel11;
	}

	/**
	 * This method initializes jPanel12	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel12() {
		if (jPanel12 == null) {
			jPanel12 = new JPanel();
			jPanel12.setLayout(null);
			jPanel12.setBounds(new Rectangle(25, 134, 265, 37));
			jPanel12.setBorder(new EtchedBorder());
			jPanel12.add(getJCheckBox3(), null);
			jPanel12.add(getJTextField9(), null);
		}
		return jPanel12;
	}

	/**
	 * This method initializes jPanel13	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel13() {
		if (jPanel13 == null) {
			jPanel13 = new JPanel();
			jPanel13.setLayout(null);
			jPanel13.setBounds(new Rectangle(25, 176, 265, 37));
			jPanel13.setBorder(new EtchedBorder());
			jPanel13.add(getJCheckBox4(), null);
			jPanel13.add(getJTextField10(), null);
		}
		return jPanel13;
	}

	/**
	 * This method initializes jPanel14	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel14() {
		if (jPanel14 == null) {
			jPanel14 = new JPanel();
			jPanel14.setLayout(null);
			jPanel14.setBounds(new Rectangle(25, 219, 265, 37));
			jPanel14.setBorder(new EtchedBorder());
			jPanel14.add(getJCheckBox5(), null);
			jPanel14.add(getJTextField11(), null);
		}
		return jPanel14;
	}

	/**
	 * This method initializes jTextField8	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJTextField8() {
		if (jTextField8 == null) {
			jTextField8 = new JTextField();
			jTextField8.setBounds(new Rectangle(98, 6, 79, 25));
			jTextField8.setEditable(false);
			jTextField8.setText("1.0");
		}
		return jTextField8;
	}

	/**
	 * This method initializes jTextField9	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJTextField9() {
		if (jTextField9 == null) {
			jTextField9 = new JTextField();
			jTextField9.setBounds(new Rectangle(100, 6, 78, 22));
			jTextField9.setEditable(false);
			jTextField9.setText("1.0");
		}
		return jTextField9;
	}

	/**
	 * This method initializes jTextField10	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJTextField10() {
		if (jTextField10 == null) {
			jTextField10 = new JTextField();
			jTextField10.setBounds(new Rectangle(101, 5, 78, 25));
			jTextField10.setEditable(false);
			jTextField10.setText("1.0");
		}
		return jTextField10;
	}

	/**
	 * This method initializes jTextField11	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJTextField11() {
		if (jTextField11 == null) {
			jTextField11 = new JTextField();
			jTextField11.setBounds(new Rectangle(102, 4, 77, 26));
			jTextField11.setEditable(false);
			jTextField11.setText("1.0");
		}
		return jTextField11;
	}

	/**
	 * This method initializes jButton4	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJButton4() {
		if (jButton4 == null) {
			jButton4 = new JButton();
			jButton4.setBounds(new Rectangle(68, 268, 96, 31));
			jButton4.setText("Search");
			jButton4.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					System.out.println(htmlCheckedNum);
					htmlMultiFiedlSearch();
					jDialog.dispose();
				}
			});
		}
		return jButton4;
	}

	/**
	 * This method initializes jButton5	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJButton5() {
		if (jButton5 == null) {
			jButton5 = new JButton();
			jButton5.setBounds(new Rectangle(170, 269, 83, 30));
			jButton5.setText("Clear");
			jButton5.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					jTextField1.setText("");
					jTextField2.setText("");
					jTextField3.setText("");
					jTextField4.setText("");
					jTextField5.setText("");
				}
			});
			
		}
		return jButton5;
	}

	/**
	 * This method initializes jButton6	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJButton6() {
		if (jButton6 == null) {
			jButton6 = new JButton();
			jButton6.setBounds(new Rectangle(183, 270, 86, 29));
			jButton6.setText("Clear");
		}
		return jButton6;
	}

	/**
	 * This method initializes jComboBox1	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox getJComboBox1() {
		if (jComboBox1 == null) {
			jComboBox1 = new JComboBox();
			jComboBox1.setBounds(new Rectangle(620, 11, 61, 33));
			
			String[] items={"HTML","PDF"};
			for(int i=0;i<items.length;i++)
			jComboBox1.addItem(items[i]);
			jComboBox1.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					if(jComboBox1.getSelectedIndex()==0)
					{
						indexMode=1;
						jComboBox.removeAllItems();
						for(int i=0;i<htmlItems.length;i++)
							jComboBox.addItem(htmlItems[i]);
					}
					else
					{
						indexMode=2;
						jComboBox.removeAllItems();
						for(int i=0;i<pdfItems.length;i++)
							jComboBox.addItem(pdfItems[i]);
					}
				}
			});
		}
		return jComboBox1;
	}

	/**
	 * This method initializes jDialog1	
	 * 	
	 * @return javax.swing.JDialog	
	 */
	private JDialog getJDialog1() {
		if (jDialog1 == null) {
			jDialog1 = new JDialog(this);
			jDialog1.setModal(true);
			jDialog1.setSize(new Dimension(356, 338));
			jDialog1.setResizable(false);
			jDialog1.setContentPane(getJContentPane11());
			jDialog1.setTitle("PDFAdvanced");
		}
		return jDialog1;
	}

	/**
	 * This method initializes jContentPane11	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJContentPane11() {
		if (jContentPane11 == null) {
			jContentPane11 = new JPanel();
			jContentPane11.setLayout(new BorderLayout());
			jContentPane11.add(getJTabbedPane1(), java.awt.BorderLayout.CENTER);
		}
		return jContentPane11;
	}

	/**
	 * This method initializes jTabbedPane1	
	 * 	
	 * @return javax.swing.JTabbedPane	
	 */
	private JTabbedPane getJTabbedPane1() {
		if (jTabbedPane1 == null) {
			jTabbedPane1 = new JTabbedPane();
			jTabbedPane1.addTab("BooleanSearch", null, getJPanel81(), null);
			jTabbedPane1.addTab("MultiFieldSearch", null, getJPanel91(), null);
		}
		return jTabbedPane1;
	}

	/**
	 * This method initializes jPanel81	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel81() {
		if (jPanel81 == null) {
			jPanel81 = new JPanel();
			jPanel81.setLayout(null);
			jPanel81.add(getJPanel31(), null);
			jPanel81.add(getJPanel61(), null);
			jPanel81.add(getJPanel71(), null);
			jPanel81.add(getJButton31(), null);
			jPanel81.add(getJButton51(), null);
		}
		return jPanel81;
	}

	/**
	 * This method initializes jPanel31	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel31() {
		if (jPanel31 == null) {
			jLabel11 = new JLabel();
			jLabel11.setBounds(new Rectangle(4, 5, 95, 28));
			jLabel11.setHorizontalAlignment(JLabel.CENTER);
			jLabel11.setText("<html><strong>TITLE</strong></html>");
			jLabel11.setBorder(new EtchedBorder());
			jPanel31 = new JPanel();
			jPanel31.setLayout(null);
			jPanel31.setBounds(new Rectangle(18, 18, 292, 46));
			jPanel31.setBorder(new EtchedBorder());
			jPanel31.add(jLabel11, null);
			jPanel31.add(getJTextField13(), null);
		}
		return jPanel31;
	}

	/**
	 * This method initializes jTextField13	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJTextField13() {
		if (jTextField13 == null) {
			jTextField13 = new JTextField();
			jTextField13.setBounds(new Rectangle(104, 4, 177, 28));
			jTextField13.setFont(new Font("Dialog", Font.BOLD, 14));
		}
		return jTextField13;
	}

	/**
	 * This method initializes jPanel61	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel61() {
		if (jPanel61 == null) {
			jLabel41 = new JLabel();
			jLabel41.setBounds(new Rectangle(10, 6, 92, 36));
			jLabel41.setHorizontalAlignment(SwingConstants.CENTER);
			jLabel41.setText("ABSTRACT");
			jLabel41.setBorder(new EtchedBorder());
			jPanel61 = new JPanel();
			jPanel61.setLayout(null);
			jPanel61.setBounds(new Rectangle(19, 71, 293, 49));
			jPanel61.setBorder(new EtchedBorder());
			jPanel61.add(jLabel41, null);
			jPanel61.add(getJTextField41(), null);
		}
		return jPanel61;
	}

	/**
	 * This method initializes jTextField41	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJTextField41() {
		if (jTextField41 == null) {
			jTextField41 = new JTextField();
			jTextField41.setBounds(new Rectangle(111, 7, 169, 37));
			jTextField41.setFont(new Font("Dialog", Font.BOLD, 14));
		}
		return jTextField41;
	}

	/**
	 * This method initializes jPanel71	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel71() {
		if (jPanel71 == null) {
			jLabel51 = new JLabel();
			jLabel51.setBounds(new Rectangle(8, 5, 95, 36));
			jLabel51.setHorizontalAlignment(SwingConstants.CENTER);
			jLabel51.setText("CONTENT");
			jLabel51.setBorder(new EtchedBorder());
			jPanel71 = new JPanel();
			jPanel71.setLayout(null);
			jPanel71.setBounds(new Rectangle(19, 134, 293, 49));
			jPanel71.setBorder(new EtchedBorder());
			jPanel71.add(jLabel51, null);
			jPanel71.add(getJTextField51(), null);
		}
		return jPanel71;
	}

	/**
	 * This method initializes jTextField51	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJTextField51() {
		if (jTextField51 == null) {
			jTextField51 = new JTextField();
			jTextField51.setBounds(new Rectangle(113, 6, 166, 36));
			jTextField51.setFont(new Font("Dialog", Font.BOLD, 14));
		}
		return jTextField51;
	}

	/**
	 * This method initializes jButton31	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJButton31() {
		if (jButton31 == null) {
			jButton31 = new JButton();
			jButton31.setBounds(new Rectangle(64, 218, 80, 31));
			jButton31.setText("Search");
			jButton31.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					pdfBooleanSearch();
					jDialog1.dispose();
				}
			});
		}
		return jButton31;
	}

	/**
	 * This method initializes jButton51	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJButton51() {
		if (jButton51 == null) {
			jButton51 = new JButton();
			jButton51.setBounds(new Rectangle(159, 219, 83, 30));
			jButton51.setText("Clear");
			jButton51.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					jTextField13.setText("");
					jTextField41.setText("");
					jTextField51.setText("");
				}
			});
		}
		return jButton51;
	}

	/**
	 * This method initializes jPanel91	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel91() {
		if (jPanel91 == null) {
			jPanel91 = new JPanel();
			jPanel91.setLayout(null);
			jPanel91.add(getJTextField61(), null);
			jPanel91.add(getJPanel101(), null);
			jPanel91.add(getJPanel131(), null);
			jPanel91.add(getJPanel141(), null);
			jPanel91.add(getJButton41(), null);
			jPanel91.add(getJButton61(), null);
		}
		return jPanel91;
	}

	/**
	 * This method initializes jTextField61	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJTextField61() {
		if (jTextField61 == null) {
			jTextField61 = new JTextField();
			jTextField61.setBounds(new Rectangle(23, 13, 250, 45));
			jTextField61.setFont(new Font("Dialog", Font.BOLD, 15));
		}
		return jTextField61;
	}

	/**
	 * This method initializes jPanel101	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel101() {
		if (jPanel101 == null) {
			jPanel101 = new JPanel();
			jPanel101.setLayout(null);
			jPanel101.setBounds(new Rectangle(23, 63, 249, 37));
			jPanel101.setBorder(new EtchedBorder());
			jPanel101.add(getJCheckBox11(), null);
			jPanel101.add(getJTextField71(), null);
		}
		return jPanel101;
	}

	/**
	 * This method initializes jCheckBox11	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getJCheckBox11() {
		if (jCheckBox11 == null) {
			jCheckBox11 = new JCheckBox();
			jCheckBox11.setBounds(new Rectangle(11, 6, 62, 25));
			jCheckBox11.setText("Title");
			jCheckBox11.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					if(jCheckBox11.isSelected())
					{
						pdfFieldState[0]=true;
						pdfCheckedNum++;
						getJTextField71().setEditable(true);
						getJTextField71().setText("1.0");
						//weight[0]
					}
					else
					{
						pdfFieldState[0]=false;
						pdfCheckedNum--;
						getJTextField71().setEditable(false);
						getJTextField71().setText("1.0");
					}
				}
			});
		}
		return jCheckBox11;
	}

	/**
	 * This method initializes jTextField71	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJTextField71() {
		if (jTextField71 == null) {
			jTextField71 = new JTextField();
			jTextField71.setBounds(new Rectangle(97, 5, 76, 28));
			jTextField71.setEditable(false);
			jTextField71.setText("1.0");
		}
		return jTextField71;
	}

	/**
	 * This method initializes jPanel131	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel131() {
		if (jPanel131 == null) {
			jPanel131 = new JPanel();
			jPanel131.setLayout(null);
			jPanel131.setBounds(new Rectangle(25, 109, 247, 36));
			jPanel131.setBorder(new EtchedBorder());
			jPanel131.add(getJCheckBox41(), null);
			jPanel131.add(getJTextField101(), null);
		}
		return jPanel131;
	}

	/**
	 * This method initializes jCheckBox41	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getJCheckBox41() {
		if (jCheckBox41 == null) {
			jCheckBox41 = new JCheckBox();
			jCheckBox41.setBounds(new Rectangle(5, 6, 86, 24));
			jCheckBox41.setText("Abstract");
			jCheckBox41.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					if(jCheckBox41.isSelected())
					{
						pdfFieldState[1]=true;
						pdfCheckedNum++;
						getJTextField101().setEditable(true);
						getJTextField101().setText("1.0");
						//weight[0]
					}
					else
					{
						pdfFieldState[1]=false;
						pdfCheckedNum--;
						getJTextField101().setEditable(false);
						getJTextField101().setText("1.0");
					}
				}
			});
		}
		return jCheckBox41;
	}

	/**
	 * This method initializes jTextField101	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJTextField101() {
		if (jTextField101 == null) {
			jTextField101 = new JTextField();
			jTextField101.setBounds(new Rectangle(95, 5, 82, 25));
			jTextField101.setEditable(false);
			jTextField101.setText("1.0");
		}
		return jTextField101;
	}

	/**
	 * This method initializes jPanel141	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel141() {
		if (jPanel141 == null) {
			jPanel141 = new JPanel();
			jPanel141.setLayout(null);
			jPanel141.setBounds(new Rectangle(26, 152, 245, 40));
			jPanel141.setBorder(new EtchedBorder());
			jPanel141.add(getJCheckBox51(), null);
			jPanel141.add(getJTextField111(), null);
		}
		return jPanel141;
	}

	/**
	 * This method initializes jCheckBox51	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getJCheckBox51() {
		if (jCheckBox51 == null) {
			jCheckBox51 = new JCheckBox();
			jCheckBox51.setBounds(new Rectangle(4, 4, 82, 31));
			jCheckBox51.setText("Content");
			jCheckBox51.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					if(jCheckBox51.isSelected())
					{
						pdfFieldState[2]=true;
						pdfCheckedNum++;
						getJTextField111().setEditable(true);
						getJTextField111().setText("1.0");
						//weight[0]
					}
					else
					{
						pdfFieldState[2]=false;
						pdfCheckedNum--;
						getJTextField111().setEditable(false);
						getJTextField111().setText("1.0");
					}
				}
			});
		}
		return jCheckBox51;
	}

	/**
	 * This method initializes jTextField111	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJTextField111() {
		if (jTextField111 == null) {
			jTextField111 = new JTextField();
			jTextField111.setBounds(new Rectangle(96, 8, 83, 26));
			jTextField111.setEditable(false);
			jTextField111.setText("1.0");
		}
		return jTextField111;
	}

	/**
	 * This method initializes jButton41	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJButton41() {
		if (jButton41 == null) {
			jButton41 = new JButton();
			jButton41.setBounds(new Rectangle(36, 228, 96, 31));
			jButton41.setText("Search");
			jButton41.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					System.out.println(pdfCheckedNum);
					pdfMultiFiedlSearch();
					jDialog1.dispose();
				}
			});
		}
		return jButton41;
	}

	/**
	 * This method initializes jButton61	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJButton61() {
		if (jButton61 == null) {
			jButton61 = new JButton();
			jButton61.setBounds(new Rectangle(151, 230, 86, 29));
			jButton61.setText("Clear");
		}
		return jButton61;
	}

	public static void main(String args[])
	{
		SimplePaperSearchUI luir=new SimplePaperSearchUI();
		luir.setVisible(true);
		
	}

}  //  @jve:decl-index=0:visual-constraint="10,10"
