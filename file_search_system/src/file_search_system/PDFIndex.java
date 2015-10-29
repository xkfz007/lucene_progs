package file_search_system;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

/**
 * @author 517
 * 
 */
public class PDFIndex {

	int flag = 0;// ȫ�ֵ�ָ�����

	Analyzer analyzer = new StandardAnalyzer();

	String pdfIndexPath = "pdfIndexes";

	String pdfFilePath = "E:\\hfz\\�ִ���Ϣ����\\TopConferences\\SIGIR\\pdf\\SIGIR";

	/**
	 * ȡ�ļ�����ķ��� ���ڵ�������ʽΪ��While not matching (Author infomation)and (lineNum<=3) add to TILE
	 */
	public Document getTile(String fullContent, Document doc) {
		// System.out.println("���� ");
		int count1 = 0;
		int count2 = 0;
		// int lineNum = 0;
		String temTile1, temTitle2, title = null;
		boolean bool = false;
		for (int i = 0; i < 2; i++) {
			if (i == 0) {
				char temp = fullContent.charAt(flag);
				while (temp == ' ' || temp == '\n' || temp == '\r') {
					flag++;
					// System.out.println(flag);
					temp = fullContent.charAt(flag);
				}
				count1 = flag;
				// System.out.println(temp);
				while (temp != '\n') {
					flag++;
					// System.out.println(flag);
					temp = fullContent.charAt(flag);
				}
				title = fullContent.substring(count1, flag - 1);
				title = title + " ";
				// System.out.println("herre"+title);
				flag++;
				count1 = flag;
			} else {

				char temp = fullContent.charAt(flag);
				while (temp != '\n') {
					flag++;
					// System.out.println(flag);
					temp = fullContent.charAt(flag);
					// System.out.println("here");
				}
				temTile1 = fullContent.substring(count1, flag);
				flag++;
				count2 = flag;
				// System.out.println(temTile1);
				char dtemp = fullContent.charAt(flag);
				// System.out.println(dtemp);
				while (dtemp != '\n') {
					flag++;
					// System.out.println(flag);
					dtemp = fullContent.charAt(flag);
					// System.out.println("����2");

				}
				temTitle2 = fullContent.substring(count2, flag + 1);
				// System.out.println(temTitle2);
				// System.out.println("hehheheh");
				// System.out.println(temTitle2.trim());
				flag++;
				if (isInstituteInfo(temTitle2) || isAbstractInfo(temTitle2) || isAddressInfo(temTitle2) || isEmailInfo(temTitle2)) {
					bool = true;
				}
				if (!bool) {
					title = title + temTile1;
					flag = count2;
					// System.out.println(content.charAt(flag));
				} else {
					flag = count1;
					// System.out.println(content.charAt(flag));
				}
			}

		}
		doc.add(new Field("title", title, Store.YES, Index.ANALYZED));
		// System.out.println("����:" + title);
		return doc;
	}

	/**
	 * ȡ���������ķ��� ���ڵ�������ʽΪ��((Creatorname)*(creatorname))RT (creatorAfflition)RT �ܸ�����ʱû��ʵ��
	 */

	/**
	 * ȡժҪ�ķ����� ժҪ��Ϣ����������Ϣ���档�ж�ժҪ��Ϣ�ĸ������£� A. 'Abstract'+Description ����û��: ��������������������B���� B.Overview+Description ժҪ����ֻ����һ�λ������ε����ݣ��ж�ժҪ��ȡ���� ���Բ������°취��
	 * �����һ�У�ֻ�����س��ͻ��з�����ôժҪ�ͳ�ȡ�� ��Ϊֹ�� �������һ�����ɹؼ���Keywords��Categories and Subject Descriptors��General Terms�� Digital. INTRODUCTION
	 */
	public Document getAbstract(String fullContent, Document doc) {
		int maxlength = 0;
		boolean bool = false;
		int count = flag;
		int num = 0;
		int numAthor = 0;
		int startPoint;
		char testChar = fullContent.charAt(flag);
		maxlength++;
		String abstractString;
		boolean endFlag = true;
		// System.out.println("here: "+testChar);
		String testTerm = null;
		String testLine = null;
		while (testChar == ' ' || testChar == '\n' || testChar == '\r') {
			bool = true;
			flag++;
			testChar = fullContent.charAt(flag);
			maxlength++;
		}
		if (bool) {
			count = flag;
			bool = false;
		}
		while (testChar != ' ' && testChar != '\n') {
			flag++;
			maxlength++;
			testChar = fullContent.charAt(flag);

		}
		testTerm = fullContent.substring(count, flag);
		// System.out.println("here: "+testTerm);
		flag++;
		maxlength++;
		count = flag;
		testChar = fullContent.charAt(flag);
		while (!testTerm.trim().equalsIgnoreCase("ABSTRACT")) {
			while (testChar == ' ' || testChar == '\n' || testChar == '\r') {
				bool = true;
				flag++;
				maxlength++;

				if (flag >= fullContent.length())
					break;
				testChar = fullContent.charAt(flag);
			}
			if (flag >= fullContent.length())
				break;
			if (bool) {
				count = flag;
				bool = false;
			}
			while (testChar != ' ' && testChar != '\n') {
				flag++;
				maxlength++;
				testChar = fullContent.charAt(flag);
				// System.out.println("22222: "+(int)testChar);
			}
			testTerm = fullContent.substring(count, flag);
			// System.out.println("here1: "+testTerm);
			// flag++;
			// count=flag;
			// testChar = content.charAt(flag);
			// System.out.println("11111: "+(int)testChar);
		}
		// System.out.println("11111: "+testChar);
		startPoint = count;
		// System.out.println("11111: "+content.charAt(startPoint));
		while (endFlag)
		// for(int i=0;i<22;i++)
		{
			// System.out.println("----------:"+content.length()+"vs"+flag);
			if (flag >= fullContent.length())
				break;
			flag++;
			count = flag;
			if (flag >= fullContent.length())
				break;
			testChar = fullContent.charAt(flag);
			while (testChar == ' ' || testChar == '\n' || testChar == '\r') {
				bool = true;
				flag++;
				if (flag >= fullContent.length())
					break;
				testChar = fullContent.charAt(flag);
			}
			if (flag >= fullContent.length())
				break;
			if (bool) {
				count = flag;
				bool = false;
			}
			while (testChar != '\n') {
				flag++;
				if (flag >= fullContent.length())
					break;
				testChar = fullContent.charAt(flag);
			}
			if (flag >= fullContent.length())
				break;
			testLine = fullContent.substring(count, flag);
			// System.out.println("11111----------: "+testLine);
			endFlag = isAbstractEnd(testLine);
		}
		abstractString = fullContent.substring(startPoint, count);
		doc.add(new Field("abstract", abstractString, Store.YES, Index.ANALYZED));
		// System.out.println("-----------------ժҪ:"+abstractString);
		flag = count;
		return doc;
	}

	/**
	 * ����ĵ������� ������ȡ��ժҪ�Ժ������ȫ��ȡ������
	 */
	public Document getContent(String fullContent, Document doc) {
		// System.out.println("*************:"+content.charAt(flag));
		int count = 0;
		char testChar;
		boolean endFlag = true;
		boolean bool = false;
		String testLine;
		String contentLine;
		int startPoint = flag--;
		while (endFlag)
		// for(int i=0;i<22;i++)
		{
			// System.out.println("----------:"+content.length()+"vs"+flag);
			if (flag >= fullContent.length())
				break;
			flag++;
			count = flag;
			if (flag >= fullContent.length())
				break;
			testChar = fullContent.charAt(flag);
			while (testChar == ' ' || testChar == '\n' || testChar == '\r') {
				bool = true;
				flag++;
				if (flag >= fullContent.length())
					break;
				testChar = fullContent.charAt(flag);
			}
			if (flag >= fullContent.length())
				break;
			if (bool) {
				count = flag;
				bool = false;
			}
			while (testChar != '\n') {
				flag++;
				if (flag >= fullContent.length())
					break;
				testChar = fullContent.charAt(flag);
			}
			if (flag >= fullContent.length())
				break;
			testLine = fullContent.substring(count, flag);
			// System.out.println("11111----------: "+testLine);
			endFlag = isContentEnd(testLine);
		}
		contentLine = fullContent.substring(startPoint, count);

		doc.add(new Field("content", contentLine, Store.YES, Index.ANALYZED));
		// System.out.println("*************����:"+contentLine);
		return doc;
	}

	/**
	 * ȥ��Reference
	 * 
	 */
	public boolean isContentEnd(String testLine) {
		boolean bool = true;
		int index = 0;
		int startPoint = index;
		String cstring;
		char testChar;
		int length = testLine.length();
		testChar = testLine.charAt(index);
		if (!Character.isDigit(testChar)) {
			return bool;
		} else {
			index++;
			startPoint = index;
			testChar = testLine.charAt(index);
			while (testChar == ' ' || testChar == '.') {
				index++;
				startPoint = index;
				if (index >= length)
					break;

				testChar = testLine.charAt(index);
			}
			while (testChar != '\n') {
				index++;
				if (index >= length)
					break;
				testChar = testLine.charAt(index);
			}
			cstring = testLine.substring(startPoint, index);

			if (cstring.trim().equalsIgnoreCase("REFERENCES"))
				bool = false;
		}
		return bool;
	}

	/*
	 * �ж��Ƿ���Abstract�Ľ���
	 */

	public boolean isAbstractEnd(String testLine) {
		boolean bool = true;
		int startPoint = 0;
		int endPoint = 0;
		char checkChar;
		String checkTerm;
		// 

		String[] check1 = { "Keywords", "Keywords:" };
		String[] check2 = { "Categories and Subject Descriptors", "Categories & Subject Descriptors" };
		String[] check3 = { "General Terms", "General Terms:" };
		String[] check4 = { "Keywords", "Keywords:" };
		String[] check5 = { "1. INTRODUCTION", "1 INTRODUCTION" };
		// int length=check1[0].length();
		/*
		 * System.out.println("2222:"+length); for(int i=0;i<length;i++) { System.out.println("---------:"+check1[0].charAt(i)); }
		 */
		checkChar = testLine.charAt(endPoint);
		// System.out.println("2222:---------"+testLine);
		while (checkChar != ':' && checkChar != '\n' && checkChar != '\r') {
			endPoint++;
			// System.out.println("4444:"+endPoint);
			checkChar = testLine.charAt(endPoint);
			// System.out.println("4444:"+checkChar);
			// System.out.println("22222: "+(int)testChar);
		}
		checkTerm = testLine.substring(startPoint, endPoint);
		if (checkTerm.trim().equalsIgnoreCase(check1[0]) || checkTerm.trim().equalsIgnoreCase(check2[0])
				|| checkTerm.trim().equalsIgnoreCase(check2[1]) || checkTerm.trim().equalsIgnoreCase(check3[0])
				|| checkTerm.trim().equalsIgnoreCase(check4[0]) || checkTerm.trim().equalsIgnoreCase(check5[0])
				|| checkTerm.trim().equalsIgnoreCase(check5[1])) {
			// System.out.println("3333:-----------------"+checkTerm);
			bool = false;
		}
		// System.out.println("2222:---------"+bool);
		return bool;
	}

	/*
	 * �ж��Ƿ������ߵĵ�λ��Ϣ
	 */
	public boolean isInstituteInfo(String tempIns) {
		boolean bool = false;
		String[] check = { "Department", "Microsoft", "Laboratory", "University", "Research", "Studies", "Center", "Dept.", "Science", "School",
				"IBM", "Computer", "Federal", "Technical", "Inc.", "Institue" };
		int posStart = 0;
		int posEnd = 0;
		char temp = tempIns.charAt(posStart);
		String term = null;
		while (temp != '\n') {
			while (temp != ' ' && temp != '\n')// temp!='\n'
			{
				posStart++;
				// System.out.println("1:"+temp);
				// System.out.println("1:"+posStart);
				temp = tempIns.charAt(posStart);
				// System.out.println("2:"+posStart);
			}
			// System.out.println("here");
			term = tempIns.substring(posEnd, posStart);
			// System.out.println("here");
			for (int i = 0; i < 16; i++) {
				if (term.trim().equalsIgnoreCase(check[i])) {
					bool = true;
					// System.out.println("isInstituteInfo::here");
					break;
				}
			}
			if (temp != '\n')
				posStart++;
			posEnd = posStart;
			temp = tempIns.charAt(posStart);
			// System.out.println("here!!");
		}

		return bool;
	}

	/*
	 * �ж����Ƿ������ߵĵ�ַ��Ϣ
	 */
	public boolean isAddressInfo(String tempAddr) {
		boolean bool = false;
		String[] check = { "Asia", "Road", "China", "Germany", "Slovenia", "Israel", "U.S.A.", "United Kingdom", "USA", "Australia", "Singapore" };
		int posStart = 0;
		int posEnd = 0;
		char temp = tempAddr.charAt(posStart);
		String term = null;
		while (temp != '\n') {
			while (temp != ' ' && temp != '\n')// temp!='\n'
			{
				posStart++;
				// System.out.println("1:"+posStart);
				temp = tempAddr.charAt(posStart);
				// System.out.println("2:"+posStart);
			}
			term = tempAddr.substring(posEnd, posStart);
			// System.out.println(term);
			for (int i = 0; i < 11; i++) {
				if (term.trim().equalsIgnoreCase(check[i])) {
					bool = true;
					// System.out.println("isAddressInfo::here");
					break;
				}
			}
			if (temp != '\n')
				posStart++;
			posEnd = posStart;
			temp = tempAddr.charAt(posStart);
			// System.out.println("here!!");
		}
		return bool;
	}

	/*
	 * �ж��Ƿ��������ַ
	 */
	public boolean isEmailInfo(String tempEmail) {
		boolean bool = false;
		int startPos = 0;
		while (tempEmail.charAt(startPos) != '\n' && tempEmail.charAt(startPos) != ' ') {
			if (tempEmail.charAt(startPos) == '@') {
				bool = true;
				// System.out.println("isEmailInfo::here");
				break;
			}
			startPos++;
		}
		return bool;
	}

	/*
	 * �ж��Ƿ�ʼժҪ������
	 */
	public boolean isAbstractInfo(String tempAbstract) {
		boolean bool = false;
		return bool;
	}


	public String createPdfIndex() {
		// String
		// path="F:\\hfz\\�ִ���Ϣ����\\TopConferences\\SIGIR\\HTML\\SIGIR2002";
		String path = "";

		String process = "";
		// Document document=new Document();
		// File indexDir = new File(pdfIndexPath);
		try {
			Analyzer luceneAnalyzer = new StandardAnalyzer();
			// Ϊ�����٣�׼���ý��������ڴ��н�����
			Directory fsDir = FSDirectory.getDirectory(pdfIndexPath);
			// 1������ʱ��ȡ
			Directory ramDir = new RAMDirectory(fsDir);
			// ���г���ʱ���� ramDir
			IndexWriter ramIndexWriter = new IndexWriter(ramDir, luceneAnalyzer, MaxFieldLength.LIMITED);

			int[] No = new int[33];
			No[0] = 1971;
			No[1] = 1978;
			for (int i = 2; i <= 32; i++)
				No[i] = No[i - 1] + 1;

			Document doc = null;
			InputStream input = null;

			PDDocument pdfDoc = null;
			File fileDir = null;

			File[] htmlFiles = null;

			PDFTextStripper pts = new PDFTextStripper();

			long startTime = new Date().getTime();
			for (int j = 0; j < 33; j++) {

				process += "<strong>Year:" + No[j] + "</strong><br>";
				path = pdfFilePath + Integer.toString(No[j]);
				fileDir = new File(path);
				htmlFiles = fileDir.listFiles();
				for (int i = 0; i < htmlFiles.length; i++) {
					String name = htmlFiles[i].getName();
					String filePath = htmlFiles[i].getAbsolutePath();
					// System.out.println(htmlFiles[i].getAbsolutePath());
					if (htmlFiles[i].isFile() && name.endsWith(".pdf") && name.startsWith("P")) {
						process += "<p>==No.&nbsp;&nbsp;" + (i + 1) + "��" + filePath + "&nbsp;&nbsp;   OK</p>";
						// System.out.println("===========��" + (i + 1) + "ƪ������Ϣ��=============");
						// System.out.println(fpath);
						input = new FileInputStream(filePath);
						// ���� pdf �ĵ�
						pdfDoc = PDDocument.load(input);
						String fullContent = pts.getText(pdfDoc);
						// ��������
						doc = new Document();
						doc = getTile(fullContent, doc);
						doc = getAbstract(fullContent, doc);
						doc = getContent(fullContent, doc);
						System.out.println(doc);
						ramIndexWriter.addDocument(doc);

						flag = 0;
						if (null != input)
							input.close();
						if (null != pdfDoc)
							pdfDoc.close();
					}

				}
			}

			// optimize()�����Ƕ����������Ż�
			ramIndexWriter.optimize();
			ramIndexWriter.close();
			// 2���˳�ʱ����
			IndexWriter fsIndexWriter = new IndexWriter(pdfIndexPath, luceneAnalyzer, true, MaxFieldLength.LIMITED);
			fsIndexWriter.addIndexesNoOptimize(new Directory[] { ramDir });

			fsIndexWriter.flush();
			fsIndexWriter.optimize();
			fsIndexWriter.close();
			// ����һ��������ʱ��
			long endTime = new Date().getTime();

			process += "It costs " + (endTime - startTime) + "milliseconds to index all the HTML files!";
			//System.out.println("�⻨����" + (endTime - startTime) + " ���������ĵ����ӵ���������ȥ!");

		} catch (Exception e) {
			e.printStackTrace();
		}
		return process;
	}

}
