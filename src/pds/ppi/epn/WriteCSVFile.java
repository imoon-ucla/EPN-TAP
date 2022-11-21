/**
 * 
 */
package pds.ppi.epn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import pds.ppi.product.ProductObservational;
import pds.ppi.product.observationarea.ObservingSystemComponent;

import org.apache.commons.cli.ParseException;
import pds.ppi.ditdos.util.PDS_DateFormat;
/**
 * @author In Sook Moon
 *
 */
public class WriteCSVFile {
	Boolean mVerbose= false;
	Options mOptions = new Options();
	private String mOverview = "Generate JSON DB by collections in PDS 4";
	private String mAcknowledge = "Development by In Sook Moon  at UCLA. ";
	private String	mVersion = "1.0.0";
	static String mOutputFolder ;
	static String mArchiveFolder;
	static String mTamplateFile;
	static ArrayList<File> collection_products = new ArrayList<File>();
	static int comStart=0, comEnd=0;
	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	String p1="([0-9]|[0-9][0-9]) (0?[0-9]?[0-9]|[1-2][0-9][0-9]|3[0-5][0-9]|36[0-6]) (2[0-3]|[0-1]?[0-9])";
	String p2="((19|20)\\d\\d) (0?[0-9]?[0-9]|[1-2][0-9][0-9]|3[0-5][0-9]|36[0-6]) "
		    +"(2[0-3]|[0-1]?[0-9]) (0?[0-9]|[0-5][0-9]) (0?[0-9]|[0-5][0-9]).[0-9]{1,3}";
	DateFormat format2_1 = new SimpleDateFormat("yyyy D HH mm ss.SSS");	
	DateFormat format2_2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	
	double dayMs = 1000 * 60 * 60 * 24;
	double J1970 = 2440588;
	double J2000 = 2451545;
	
	DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	DateFormat dbformatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		WriteCSVFile  me =new WriteCSVFile();
		me.loadOptions("archive.options");
		CommandLineParser parser = new PosixParser();
		CommandLine line;
			   try {
				  line = parser.parse(me.mOptions, args);
				  for(Option o : line.getOptions()) {
				      me.setOption(o.getOpt(), o.getValue());
				  }
				  if(me.mOutputFolder  == null) { System.out.println("No out file specified."); return; }
				  me.writeEPN_TAPFile();
				  
			   } catch (ParseException e) {
				// TODO Auto-generated catch block
				  e.printStackTrace();
			   } 
	}
	public WriteCSVFile(){
		mOptions.addOption( "o", "outputFolder", true, "OutputFolderLocation" ); 
		mOptions.addOption( "a", "archiveFolder", true, "ArchiveFolderLocation for data collection" ); 
		mOptions.addOption( "t", "tamplateFile", true, "Tamplate file for resource file." );
		mOptions.addOption( "h", "help", false, "Dispay this text" );
		
	}
	public void showHelp(){
		System.out.println("");
		System.out.println(getClass().getName() + "; Version: " + mVersion);
		System.out.println(mOverview);
		System.out.println("");
		System.out.println("Usage: java " + getClass().getName() + " [options] [file...]");
		System.out.println("");
		System.out.println("Options:");
			
			// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(getClass().getName(), mOptions);

		System.out.println("");
		System.out.println("Acknowledgements:");
		System.out.println(mAcknowledge);
		System.out.println("");
	}
	public boolean loadOptions(String pathname) 
	{
		if(mVerbose) System.out.println("Loading options: " + pathname);
		
		
		InputStream input = null;
		input = getClass().getClassLoader().getResourceAsStream(pathname);
		if (input == null) {
			System.out.println("Sorry, unable to find " + pathname);
			return false;
		}
 
		Properties prop = new Properties();
		try {
			prop.load(input);
			
			@SuppressWarnings("unchecked")
			Collection<Option> collection = mOptions.getOptions();
			
			// Look for all the defined options
			for(Object item : collection.toArray()) {
				Option opt = (Option) item;
				String value = prop.getProperty(opt.getLongOpt());
				if(value != null) setOption(opt.getOpt(), value);
				
			}
		} catch(Exception e) {
			return false;
		}
		
		return true;
	}
	
	public void setOption(String opt, String value){
		//if(opt.equals("h")) showHelp();
		if(opt.equals("v")) mVerbose = true;
		if(opt.equals("x")) loadOptions(value);
		if(opt.equals("o")) mOutputFolder = value;
		if(opt.equals("t")) mTamplateFile = value;
		//if(opt.equals("s")) mStartsWith = value;
		if(opt.equals("a")) mArchiveFolder = value;
		//if(opt.equals("l")) mFileLocation=value;
		
	}
	public void writeEPN_TAPFile(){
		File file =new File(mArchiveFolder);
		//Create output folder if it is not exist
		File outputfolder = new File(mOutputFolder);
		if(!outputfolder.exists()) outputfolder.mkdir();
		File csvgzfile = new File(mOutputFolder+"/data.csv");
		File rdfile = new File(mOutputFolder+"/ppi.rd");
		System.out.println(file.getName());
		
		FindProductData(file);
		System.out.println(collection_products.size());
		
		
		if(collection_products.size()>0){
			
			//write resource file. ppi.rd
			
			try {
				if(!csvgzfile.exists()) csvgzfile.createNewFile();
				FileOutputStream fos__ = new FileOutputStream(csvgzfile);
				
				String header ="granule_uid,granule_gid,obs_id,target_name,target_class,"
						+ "time_min,time_max,instrument_host_name,instrument_name,access_url,access_format,access_estsize,"
						+ "creation_date,modification_date,release_date\n";
				fos__ .write(header.getBytes());
				
				for(File f: collection_products){
					System.out.println(f.getName());
					ProductObservational po__  = new ProductObservational();
					po__.setFile(f.getAbsolutePath());
					po__.readLable();
					//pds.ppi.product.filearea.Table tbl__ = po__.getFileAreaObervational().getTable().get(0);
					
					String datafile = po__.getFileAreaObervational().getFile().getFileName();
					String line = "";
					String id = po__.getIdentifiactionArea().getLogicalIdentifier();
					String[] ids = id.split(":");
					String bundle_name = ids[3];
					
					String collection_id = id.substring(0, id.lastIndexOf(":"));
					line+=id+",";
					line+=collection_id+",";
					line+=id+", ";
					//find bundle folder;
					String bundle_folder=bundle_name;
					
					String folder_name = f.getParent().replace("\\", "/");
					String[] temp = folder_name.split("/");
					for(int i=0; i<temp.length; i++){
						if(temp[i].equals("data")){
							bundle_folder =temp[i+1];
							break;
						}
					}
					System.out.println(bundle_folder);
					String archive_path = folder_name.split(bundle_folder)[1];
					//System.out.println(archive_path);		
					
					//write target_name
					if(po__.getObservationArea().getTargetIdentifications()!=null){
						line+=po__.getObservationArea().getTargetIdentifications().get(0).getName();
					}
					line+=",";
					//write target_class
					if(po__.getObservationArea().getTargetIdentifications().get(0)!=null){
						line+=po__.getObservationArea().getTargetIdentifications().get(0).getType();
					}
					line+=",";
					
					String startdate = po__.getObservationArea().getTimeCoordinates().getStartDateTime();
					String stopdate  = po__.getObservationArea().getTimeCoordinates().getStopDateTime();
					if(startdate!=null){
						Date d_startdate = PDS_DateFormat.parse(startdate);
						if(d_startdate!=null){
							double time_min = this.toJulian(d_startdate);
							line+=time_min+",";
						}
						else{
							line+=",";
						}
					}
					else{
						line+=",";
					}
					if(stopdate!=null){
						Date d_startdate = PDS_DateFormat.parse(startdate);
						if(d_startdate!=null){
							double time_min = this.toJulian(d_startdate);
							line+=time_min+",";
						}
						else{
							line+=",";
						}
					}
					else{
						line+=",";
					}
					String instrument =null, instrument_host =null;
					
					if(po__.getObservationArea().getObservingSystem().getObservingSystemComponent()!=null){
						for(ObservingSystemComponent osc__ : po__.getObservationArea().getObservingSystem().getObservingSystemComponent()){
							if(osc__.getType().equalsIgnoreCase("instrument")) instrument =osc__.getName();
							if(osc__.getType().equalsIgnoreCase("instrument_host") || osc__.getType().equalsIgnoreCase("host")) instrument_host = osc__.getName();
						}
					}
					if(instrument_host!=null){
						line += instrument_host;
					}
					line+=",";
					if(instrument!=null){
						line += instrument;
					}
					line+=",";
					
					String access_url="https://pds-ppi.igpp.ucla.edu/data/"+bundle_folder+archive_path+"/"+datafile;
					line+=access_url+",";
					
					String format = "text/plain";
					if(datafile.endsWith(".cdf")){
						format ="application/x-binary+cdf";
					}
					else if(datafile.toLowerCase().endsWith(".dat") && po__.getFileAreaObervational().getTable().get(0).getDataType().equalsIgnoreCase("binary")){
						format="application/octet-stream";
					}
					line+=format+",";
					File dfile = new File(f.getParent().replace("\\", "/")+"/"+datafile);
					
					long etssize =dfile.length()/1024;
					line+=etssize+",";
					Date today = new Date();
					line +=dbformatter.format(today)+",";
					line +=dbformatter.format(today)+",";
					line +=dbformatter.format(today)+"\n";
				
					System.out.println(line);
					fos__.write(line.getBytes());
				}
				
				fos__.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			
		}
		else{
			System.out.println("Cannot find any produt file for this collection");
		}
	}
	public void writeResource(){}	
	public Document getResourceFile(String filename){
		DocumentBuilder db;
		try {
			db = dbf.newDocumentBuilder();
			Document doc = db.parse(new File(filename));
			return doc;
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        
        
        return null;

	}
	public double toJulian(Date date) {  
        double result =date.toInstant().toEpochMilli(); //milliseconds since epoch
        result = result / dayMs - 0.5 + J1970;
        return result;
    }
	public void writeCSVFile(){
		File file =new File(mArchiveFolder);
		String csvgzfile = mOutputFolder+"/data.csv.gz";
		//read data
		System.out.println(file.getName());
		FindProductData(file);
		System.out.println(collection_products.size());
		File collection__ = this.FindCollectionFile(file);
		System.out.println("collection:" +collection__);
		String p1="([0-9]|[0-9][0-9]) (0?[0-9]?[0-9]|[1-2][0-9][0-9]|3[0-5][0-9]|36[0-6]) (2[0-3]|[0-1]?[0-9])";
		String p2="((19|20)\\d\\d) (0?[0-9]?[0-9]|[1-2][0-9][0-9]|3[0-5][0-9]|36[0-6]) "
			    +"(2[0-3]|[0-1]?[0-9]) (0?[0-9]|[0-5][0-9]) (0?[0-9]|[0-5][0-9]).[0-9]{1,3}";
		DateFormat format2_1 = new SimpleDateFormat("yyyy D HH mm ss.SSS");	
		DateFormat format2_2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		DateFormat format2_3 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");
		if(collection_products.size()>0){
			try {
				GZIPOutputStream gos = new GZIPOutputStream(new FileOutputStream(new File(csvgzfile)));
				
				String header = this.getHeader();
				System.out.println(header);
				String line;
				
				for(File f: collection_products){
					System.out.println(f.getName());
					ProductObservational po__  = new ProductObservational();
					po__.setFile(f.getAbsolutePath());
					po__.readLable();
					pds.ppi.product.filearea.Table tbl__ = po__.getFileAreaObervational().getTable().get(0);
					
					System.out.println( po__.getFileAreaObervational().getFile().getFileName());
					String datafile = po__.getFileAreaObervational().getFile().getFileName();
					//System.out.println(f.getParent().replace("\\", "/"));
					File dataFile = new File(f.getParent().replace("\\", "/")+"/"+datafile);
					BufferedReader br = new BufferedReader(new FileReader(dataFile));
					br.skip(tbl__.getOffset());
					  
					while ((line = br.readLine()) != null) {
						String record="";
						for(int i=0; i<tbl__.getRecord().getFields(); i++){
							if(comEnd>0 && i==comStart){
								//read date value
								
								String datevalue="";
								for(int j=comStart; j<=comEnd; j++){
									pds.ppi.product.filearea.Field f__ =tbl__.getRecord().getField().get(j);
									datevalue += line.substring(f__.getFieldLocation()-1, f__.getFieldLocation()-1+f__.getFieldLength()).trim()+" ";
									
								}
								datevalue=datevalue.trim();
								
								if(datevalue.matches(p2)){
									String value=format2_2.format(format2_1.parse(datevalue));
									record+=value+",";
								}
								i=comEnd;
							}
							else{
								pds.ppi.product.filearea.Field f__ =tbl__.getRecord().getField().get(i);
								String value=line.substring(f__.getFieldLocation()-1, f__.getFieldLocation()-1+f__.getFieldLength()).trim();
								record+=value+",";
							}
						}
						record=record.substring(0, record.length()-1)+"\n";
						gos.write(record.getBytes());
						
					}
				}
				
				gos.close();
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (java.text.ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}	
	}
	public String getHeader(){
		String header="";
		ProductObservational po__  = new ProductObservational();
		po__.setFile(collection_products.get(0).getAbsolutePath());
		po__.readLable();
		pds.ppi.product.filearea.Table tbl__ = po__.getFileAreaObervational().getTable().get(0);
		
		pds.ppi.product.filearea.Record rec__ = tbl__.getRecord();
		
		for(int i=0; i<rec__.getFields(); i++){
			pds.ppi.product.filearea.Field f__ = rec__.getField().get(i);
			if(f__.getName().equalsIgnoreCase("year")){
				comStart=i;
				for(int j=i; j<rec__.getFields(); j++){
					if( rec__.getField().get(j).getName().equalsIgnoreCase("DAY_OF_YEAR")){
						i=j;
						comEnd=j;
					}
					if( rec__.getField().get(j).getName().equalsIgnoreCase("HOUR")){
						i=j;
						comEnd=j;
					}
					if( rec__.getField().get(j).getName().equalsIgnoreCase("MINUTE")){
						i=j;
						comEnd=j;
					}
					if( rec__.getField().get(j).getName().equalsIgnoreCase("SECOND")){
						i=j;
						comEnd=j;
					}
				}
				header +="UTC,";
			}
			else{
				header+=f__.getName()+',';
			}
			
			
		}
		header = header.substring(0, header.length()-1);
		return header;
	}
	public static File FindCollectionFile(File directory){
		for(File f: directory.listFiles()){
			if(f.isFile() && (f.getName().toLowerCase().startsWith("collection") && f.getName().toLowerCase().endsWith(".xml"))){
				return f;
			}
		}
		return null;
	}
	public static void FindProductData(File directory ){
		File[] files = directory.listFiles(new ProductFilter());
		File[] folders = directory.listFiles(new DirectoryFilter());
		boolean hasColFile=false;
		if(files!=null){
			for(int i=0; i<files.length; i++){
				if(!(files[i].getName().toLowerCase().startsWith("collection")&&files[i].getName().toLowerCase().endsWith(".xml"))){
					collection_products.add(files[i]);
				}
			}
		}
		if(folders!=null){
			for(File f: folders){
				FindProductData(f);
			}
		}
		
	}
}

class ProductFilter implements FileFilter {
    public boolean accept(File file) {
    	return  !file.getName().toLowerCase().startsWith("collection") && file.getName().endsWith(".xml");
    }
}
class DirectoryFilter implements FileFilter {
    public boolean accept(File file) {
    	if(file.isDirectory() && !file.getName().toLowerCase().startsWith(".mimic")  && !file.getName().equalsIgnoreCase("WEB-INF"))
    		return true;
    	else return false;
    }
}
