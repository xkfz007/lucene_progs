package file_search_system;

import java.io.File;
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
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.HasChildFilter;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.filters.StringFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;


public class HTMLIndex {

	// ����·��
	String htmlFilePath = "E:\\hfz\\�ִ���Ϣ����\\TopConferences\\SIGIR\\HTML\\SIGIR";

	// String htmlIndexPath = "E:\\hfz\\�ִ���Ϣ����\\����ҵ\\����2\\SmallIR\\sigirIndexes";
	// ʹ�����·��
	String htmlIndexPath = "htmlIndexes";

	// �ַ�����ʽ������
	public String strOp(String str) {
		String s1 = str.trim().replace("&nbsp;", "").replace("&amp;", "");
		s1 = s1.replaceAll("\\(.+\\)", "");
		return s1;
	}

	// �ڱ���л�ȡ��������������Ϣ
	public Document getTitleAndAuthor(String urlStr, Document doc) {
		try {
			NodeFilter filter = new NodeClassFilter(TableTag.class);

			Parser parser = new Parser();
			NodeList nodelist = null;

			// ����url��ַ
			parser.setURL(urlStr);
			parser.setEncoding(parser.getEncoding());

			// ��ȡ���⣬���ߺ����߹�����λ��Ϣ
			nodelist = parser.extractAllNodesThatMatch(filter);
			if (nodelist.size() > 0) {
				// ������0-14�еĵ�7��
				TableTag tableTitle = (TableTag) nodelist.elementAt(7);
				// System.out.println(table);
				TableRow rowTitle = tableTitle.getRow(0);
				String title = strOp(rowTitle.toPlainTextString());

				// ���title
				doc.add(new Field("title", title, Store.YES, Index.ANALYZED));// ,TermVector.WITH_POSITIONS_OFFSETS));
				// System.out.println("����Ϊ��" + title);

				// ������Ϣʱ�ڵ�8��
				TableTag tableAuthor = (TableTag) nodelist.elementAt(8);
				// System.out.println(tableAuthor.toPlainTextString());
				TableRow[] rowAuthor = tableAuthor.getRows();
				int rowcount = rowAuthor.length;
				// ��ʼ��
				String[] authors = new String[rowcount];
				String[] workplace = new String[rowcount];
				for (int i = 0; i < rowcount; i++) {
					authors[i] = "";
					workplace[i] = "";
				}
				TableRow tmprow = null;
				TableColumn authorCol = null, workCol = null;
				for (int i = 0; i < rowcount; i++) {
					tmprow = rowAuthor[i];
					// ����ط�Ҫע��
					if (tmprow.getColumnCount() > 1) {
						authorCol = tmprow.getColumns()[0];
						workCol = tmprow.getColumns()[1];
						authors[i] = strOp(authorCol.toPlainTextString());
						workplace[i] = strOp(workCol.toPlainTextString());// .trim().replace("&nbsp;",
						// "");
					}
					// ������Termvertor����
					doc.add(new Field("author", authors[i], Store.YES, Index.ANALYZED));// ,TermVector.WITH_POSITIONS_OFFSETS));
					doc.add(new Field("workplace", workplace[i], Store.YES, Index.ANALYZED));// ,TermVector.WITH_POSITIONS_OFFSETS));

					// System.out.println("��" + (i + 1) + "������Ϊ��" + authors[i]
					// + ",���ߵĹ�����λΪ��" + workplace[i]);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return doc;
	}

	// ��ȡժҪ��Ϣ��û�еĻ��������ַ���
	public Document getAbstract(String urlStr, Document doc) {
		Parser parser = new Parser();
		// ����url��ַ
		try {
			parser.setURL(urlStr);
			parser.setEncoding(parser.getEncoding());
			// NodeFilter classAbstract=new
			// HasAttributeFilter("class","abstract");
			NodeFilter nameAbstract = new HasAttributeFilter("name", "abstract");
			// NodeFilter filter2And=new AndFilter(classAbstract,nameAbstract);

			// NodeFilter childFilter1=new HasChildFilter(classAbstract);
			NodeFilter childFilter2 = new HasChildFilter(nameAbstract);
			NodeFilter divFilter = new TagNameFilter("DIV");

			NodeFilter pFilter = new TagNameFilter("p");
			NodeFilter spanFilter = new TagNameFilter("span");
			NodeFilter spchild = new AndFilter(spanFilter, childFilter2);

			NodeFilter childFilter3 = new HasChildFilter(spchild);

			NodeList nodelist = null;

			nodelist = parser.extractAllNodesThatMatch(new AndFilter(divFilter, childFilter3));

			String abt = "", tmps;
			int len = 0;
			// System.out.println(nodelist.size());
			Node divNode = nodelist.elementAt(0);
			NodeList nodelist2 = new NodeList();
			if (divNode != null) {
				divNode.collectInto(nodelist2, pFilter);
				for (int i = 0; i < nodelist2.size(); i++) {

					Node node = nodelist2.elementAt(i);

					if (node != null) {

						// paraList.add(tag.getText());
						// System.out.println(node.getText());
						// System.out.println(tag.toPlainTextString().trim());
						// System.out.println((i+1)+"***********************************");
						tmps = node.toPlainTextString().trim();
						// tmps=strOp(tmps);
						int tmpn = tmps.length();
						if (tmpn >= len) {
							len = tmpn;
							// K=i;
							abt = tmps;
						}
					}

				}

			}

			// ��ժҪ�����ַ����任
			abt = abt.replace("&nbsp;", "").replace("&amp;", "").replace("&rdquo;", "");
			// ������Termvertor����
			doc.add(new Field("abstract", abt, Store.YES, Index.ANALYZED));// ,TermVector.WITH_POSITIONS_OFFSETS));
			// System.out.println("ժҪ��ϢΪ��" + abt);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return doc;
	}

	// ��ȡ���ķ���ʱ��
	public Document getYearofPublication(String urlStr, Document doc) {
		Parser parser = new Parser();
		// ����url��ַ
		try {
			parser.setURL(urlStr);
			parser.setEncoding(parser.getEncoding());
			NodeFilter yearFilter = new StringFilter("Year of Publication:");
			NodeList nodelist = null;
			nodelist = parser.extractAllNodesThatMatch(yearFilter);
			Node yearNode = nodelist.elementAt(0);
			String year = yearNode.toPlainTextString().trim();
			year = strOp(year);
			int ind = year.indexOf(":");
			year = year.substring(ind + 1, ind + 5);
			doc.add(new Field("year", year, Store.YES, Index.ANALYZED));
			System.out.println(year);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return doc;
	}

	// ���������ķ���

	public String createHtmlIndex() {

		String path = "";

		String process = "";
		Analyzer luceneAnalyzer = null;

		Directory fsDir = null, ramDir = null;

		IndexWriter ramIndexWriter = null;

		IndexWriter fsIndexWriter = null;

		Document doc = null;
		try {
			luceneAnalyzer = new StandardAnalyzer();
			// Ϊ�����٣�׼���ý��������ڴ��н�����
			fsDir = FSDirectory.getDirectory(htmlIndexPath);
			// 1������ʱ��ȡ
			ramDir = new RAMDirectory(fsDir);
			// ���г���ʱ���� ramDir
			ramIndexWriter = new IndexWriter(ramDir, luceneAnalyzer, MaxFieldLength.LIMITED);

			int[] No = new int[33];
			No[0] = 1971;
			No[1] = 1978;
			for (int i = 2; i <= 32; i++)
				No[i] = No[i - 1] + 1;

			long startTime = new Date().getTime();
			File fileDir = null;
			File[] htmlFiles = null;
			for (int j = 0; j < 33; j++) {

				process += "<strong>Year:" + No[j] + "</strong><br>";
				path = htmlFilePath + Integer.toString(No[j]);
				fileDir = new File(path);
				htmlFiles = fileDir.listFiles();
				for (int i = 0; i < htmlFiles.length; i++) {
					String name = htmlFiles[i].getName();
					String filePath = htmlFiles[i].getAbsolutePath();
					// System.out.println(htmlFiles[i].getAbsolutePath());
					if (htmlFiles[i].isFile() && name.endsWith(".html") && name.startsWith("P")) {
						process += "<p>==No.&nbsp;&nbsp;" + (i + 1) + "��" + filePath + "&nbsp;&nbsp;   OK</p>";
						// System.out.println(filePath);
						// System.out.println("===========��" + (i + 1) + "ƪ������Ϣ��=============");
						// System.out.println(filePath);
						// ��������
						doc = new Document();
						doc = getTitleAndAuthor(filePath, doc);
						doc = getAbstract(filePath, doc);
						doc = getYearofPublication(filePath, doc);
						// System.out.println(doc);
						ramIndexWriter.addDocument(doc);
					}

				}
			}

			// optimize()�����Ƕ����������Ż�
			ramIndexWriter.optimize();
			ramIndexWriter.close();
			// 2���˳�ʱ����
			fsIndexWriter = new IndexWriter(htmlIndexPath, luceneAnalyzer, true, MaxFieldLength.LIMITED);
			fsIndexWriter.addIndexesNoOptimize(new Directory[] { ramDir });

			fsIndexWriter.flush();
			fsIndexWriter.optimize();
			fsIndexWriter.close();
			// ����һ��������ʱ��
			long endTime = new Date().getTime();

			process += "It costs " + (endTime - startTime) + "milliseconds to index all the HTML files!";
			// System.out.println("�⻨����" + (endTime - startTime) + " ���������ĵ����ӵ���������ȥ!");

		} catch (Exception e) {
			e.printStackTrace();
		}
		return process;
	}

}
