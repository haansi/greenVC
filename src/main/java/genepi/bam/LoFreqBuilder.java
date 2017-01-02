package genepi.bam;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import genepi.io.table.TableReaderFactory;
import genepi.io.table.reader.CsvTableReader;
import genepi.io.table.reader.ITableReader;
import genepi.objects.CheckEntry;
import genepi.objects.HSDEntry;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.CloserUtil;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;

public class LoFreqBuilder {
	


	private String variantfile;
	private String outfile;
	private double vaf;


	public LoFreqBuilder(String variantfile, String outfile) {
		this.variantfile = variantfile;
		this.outfile = outfile;
	}

	public int build() throws MalformedURLException, IOException {
	
	FileInputStream fstream;
		try {
			
			//index not required, therefore false
			VCFFileReader vcfReader = new VCFFileReader(new File(variantfile), false);
			
		
			CloseableIterator<VariantContext> variantIter  = vcfReader.iterator();

			HashMap<String, ArrayList<CheckEntry>> hm = new HashMap<String, ArrayList<CheckEntry>>();
			FileWriter fw;
			fw = new FileWriter(new File(outfile));
			fw.write("SampleID\tRange\tHaplogroup\tPolymorphisms");
			fw.write(System.lineSeparator());
			try {
				while (variantIter.hasNext()) {
					final VariantContext vc = variantIter.next();
					CheckEntry entry = new CheckEntry();
					String id = variantfile;
					entry.setID(id);
					entry.setPOS(vc.getStart());
					entry.setREF(vc.getReference().getBaseString());
					double allelefreq = Double.valueOf(vc.getAttribute("AF").toString());
					entry.setVAF(allelefreq);
					if ((allelefreq)>0.50){
						entry.setBaseMajor(vc.getAlleles().get(1).getBaseString());
						entry.setBaseMinor(vc.getAlleles().get(0).getBaseString());
					}
					else{
						entry.setBaseMajor(vc.getAlleles().get(0).getBaseString());
						entry.setBaseMinor(vc.getAlleles().get(1).getBaseString());
					}		
				

					if (hm.containsKey(id)) {
						hm.get(id).add(entry);
					} else if (hm.get(id) == null) {
						hm.put(id, new ArrayList<CheckEntry>());
						hm.get(id).add(entry);
					}
				}
			
			} catch (Exception e) {
				System.out.println("Column names not present as #CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO:DP;AF;SB;DP4;CONSVAR ");
				e.printStackTrace();
			}
			
	        CloserUtil.close(variantIter);
	        CloserUtil.close(vcfReader);
			

			Iterator it = hm.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pair = (Map.Entry) it.next();
				HSDEntry minor = new HSDEntry();
				HSDEntry major = new HSDEntry();
				minor.setID(pair.getKey() + "_min");
				StringBuffer range = new StringBuffer();
				
				major.setID(pair.getKey() + "_maj");
				

				ArrayList<CheckEntry> helpArray = hm.get(pair.getKey());
				for (int i = 0; i < helpArray.size(); i++) {

					if (helpArray.get(i).getREF().contains("-") || helpArray.get(i).getREF().equals("N")) {
						// skip indel, and 3107 on rCRS;
					} else {
						
						if (helpArray.get(i).getVAF() < 0.5) {
							range.append(helpArray.get(i).getPOS() + ";");
							if (helpArray.get(i).getREF().equals(helpArray.get(i).getPOS())) {
								minor.appendPROFILES(helpArray.get(i).getPOS() + helpArray.get(i).getBaseMinor()); //+ " " + helpArray.get(i).getVAF());
								major.appendPROFILES(helpArray.get(i).getPOS() + helpArray.get(i).getBaseMajor()); //+ " " + helpArray.get(i).getVAF());
							} else {
								minor.appendPROFILES(helpArray.get(i).getPOS() + helpArray.get(i).getBaseMinor()); //+ " " + helpArray.get(i).getVAF());
								major.appendPROFILES(helpArray.get(i).getPOS() + helpArray.get(i).getBaseMajor()); //+ " " + helpArray.get(i).getVAF());
							}
						} else if (helpArray.get(i).getVAF() > 1-vaf) {
							range.append(helpArray.get(i).getPOS() + ";");
							if (helpArray.get(i).getREF().equals(helpArray.get(i).getPOS())) {
								minor.appendPROFILES(helpArray.get(i).getPOS() + helpArray.get(i).getBaseMajor()); // + " " + helpArray.get(i).getVAF());
								major.appendPROFILES(helpArray.get(i).getPOS() + helpArray.get(i).getBaseMinor()); // + " " + helpArray.get(i).getVAF());
							} else {
								minor.appendPROFILES(helpArray.get(i).getPOS() + helpArray.get(i).getBaseMajor()); //+ " " + helpArray.get(i).getVAF());
								major.appendPROFILES(helpArray.get(i).getPOS() + helpArray.get(i).getBaseMinor()); //+ " " + helpArray.get(i).getVAF());
							}
						}/*
						//TODO recheck if homoplasmies are neccessary
						 else { // add fixed homoplasmies VAF == 1
							minor.appendPROFILES(helpArray.get(i).getPOS() + helpArray.get(i).getBaseMajor()); //+ " " + helpArray.get(i).getVAF());
							major.appendPROFILES(helpArray.get(i).getPOS() + helpArray.get(i).getBaseMajor()); //+ " " + helpArray.get(i).getVAF());
						}*/ 
			
					}
					
					//use only occurrences
					minor.setRANGE(range.toString()); 
					major.setRANGE(range.toString());
										
					//minor.setRANGE("1-16569");
					//major.setRANGE("1-16569");
			
				}
				fw.write(minor.getString());
				fw.write(System.lineSeparator());
				fw.write(major.getString());
				fw.write(System.lineSeparator());
				it.remove(); // avoids a ConcurrentModificationException
			}
			fw.close();
			System.out.println("File written - you can now open the result file in HaploGrep2");
		} catch (Exception e) {
			System.out.println("ERROR");
			e.printStackTrace();
		} 
		// Everything fine
		return 0;
	}
	
	
	
	

	public String getOutfile() {
		return outfile;
	}

	public void setOutfile(String outfile) {
		this.outfile = outfile;
	}

	public double getVaf() {
		return vaf;
	}

	public void setVaf(double vaf) {
		this.vaf = vaf;
	}

}
	
