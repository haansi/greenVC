package genepi.bam;

import java.io.IOException;
import java.net.MalformedURLException;

import genepi.base.Tool;

public class BAMReader extends Tool {

	public BAMReader(String[] args) {
		super(args);
	}

	@Override
	public void init() {

		System.out
				.println("Perform a naive variant calling from your BAM file\n\n");

	}

	@Override
	public void createParameters() {

		addParameter("in", 	"input BAM/CRAM file (e.g. HG00096.bam)");
		addParameter("out", "output directory");
		addParameter("ref",	"input reference as fasta file (e.g. chrM.fasta)");
		addParameter("VAF",	" set the Variant Allele Frequency (VAF), example [0.1] = 10%)", DOUBLE);
		addParameter("QUAL"," set the per Base Quality, [0-40], considered [>=20 && <=35] ", INTEGER);
		addParameter("MAPQ",": set the per Base Quality, [0-255], considered [200]", INTEGER);
	}

	@Override
	public int run() {
		
		String in = (String) getValue("in");
		String out = (String) getValue("out");
		String ref = (String) getValue("ref");
		double vaf = (Double) getValue("VAF");
		int qual = (Integer) getValue("QUAL");
		int mapqual = (Integer) getValue("QUAL");

		VariantBuilder builder = new VariantBuilder(in);
		builder.setReference(ref);
		builder.setOutDirectory(out);
		builder.setVaf(vaf);
		builder.setQual(qual);
		builder.setMapqual(mapqual);


		try {
			return builder.build();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
	}

	public static void main(String[] args) {
		new BAMReader(args).start();
	}

}
