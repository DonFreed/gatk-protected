package org.broadinstitute.sting.gatk.walkers.reducereads;

import net.sf.samtools.*;
import org.broadinstitute.sting.utils.BaseUtils;

import java.util.LinkedList;
import java.util.List;

/**
 * Running Consensus is a read that is composed as a sliding window travels over the reads
 * and keeps track of all the bases that are outside of variant regions.
 *
 * Consensus reads have qual fields that correspond to the number of reads that had the base
 * and passed the minimum quality threshold.
 *
 * The mapping quality of a consensus read is the RMS of the mapping qualities of all reads
 * that compose the consensus
 *
 * @author Mauricio Carneiro
 * @since 8/26/11
 */
public class RunningConsensus {
    private List<Byte> counts;
    private List<Byte> bases;
    private double rms;   // todo -- implement this

    // Information to produce a SAMRecord
    private SAMFileHeader header;
    private Object readGroupAttribute;
    private String contig;
    private int contigIndex;
    private String readName;
    private Integer refStart;

    /**
     * Initialize your running consensus if you don't know yet what the first base and it's name
     * are going to be.
     *
     * @param header
     * @param readGroupAttribute
     * @param contig
     * @param contigIndex
     */
    public RunningConsensus (SAMFileHeader header, Object readGroupAttribute, String contig, int contigIndex) {
        this(header, readGroupAttribute, contig, contigIndex, null, null);
    }

    /**
     * Full initialization of the running consensus if you have all the information and are ready to
     * start adding to the running consensus.
     *
     * @param header
     * @param readGroupAttribute
     * @param contig
     * @param contigIndex
     * @param readName
     * @param refStart
     */
    public RunningConsensus (SAMFileHeader header, Object readGroupAttribute, String contig, int contigIndex, String readName, Integer refStart) {
        counts = new LinkedList<Byte>();
        bases = new LinkedList<Byte>();
        this.rms = 0.0;

        this.header = header;
        this.readGroupAttribute = readGroupAttribute;
        this.contig = contig;
        this.contigIndex = contigIndex;
        this.readName = readName;
        this.refStart = refStart;
    }

    public void setReadName(String readName) {
        this.readName = readName;
    }

    public void setRefStart(Integer refStart) {
        this.refStart = refStart;
    }

    /**
     * Easy access to keep adding to a running consensus that has already been
     * initialized with the correct read name and refStart
     *
     * @param base
     * @param count
     */
    public void add(byte base, byte count) {
        counts.add(count);
        bases.add(base);
    }

    public SAMRecord close () {
        SAMRecord samRecord = new SAMRecord(header);
        samRecord.setAttribute("RG", readGroupAttribute);
        samRecord.setReferenceName(contig);
        samRecord.setReferenceIndex(contigIndex);
        samRecord.setReadPairedFlag(false);
        samRecord.setReadUnmappedFlag(false);
        samRecord.setAlignmentStart(refStart);
        samRecord.setCigar(buildCigar());
        samRecord.setReadName(readName);
        return samRecord;
    }

    public int size () {
        return bases.size();
    }

    private Cigar buildCigar() {
        LinkedList<CigarElement> cigarElements = new LinkedList<CigarElement>();
        CigarOperator cigarOperator = null;
        int length = 0;
        for (Byte b : bases) {
            CigarOperator op;
            switch (BaseIndex.byteToBase(b)) {
                case D:
                    op = CigarOperator.DELETION;
                    break;
                case I:
                    op = CigarOperator.INSERTION;
                    break;
                default:
                    op = CigarOperator.MATCH_OR_MISMATCH;
                    break;
            }
            if (cigarOperator != op) {
                cigarElements.add(new CigarElement(length, cigarOperator));
                cigarOperator = op;
                length = 0;
            }
            length++;
        }
        if (length > 0)
            cigarElements.add(new CigarElement(length, cigarOperator));
        return new Cigar(cigarElements);
    }
}
