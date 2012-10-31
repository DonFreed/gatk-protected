#!/bin/tcsh

reuse Python-2.6

setenv DATE `date +"%m_%d_%Y"`
setenv ROOT /humgen/gsa-hpprojects/GATK/reports/s3
setenv GATK /home/radon01/depristo/dev/GenomeAnalysisTK/unstable
setenv DIR $ROOT/archive/$DATE

#
# manageGATKS3Logs.py copied from GATK repository, do not modify it here
#
setenv BASE "python $GATK/private/python/manageGATKS3Logs.py -b GATK_Run_Reports -s $ROOT/s3cmd-1.0.0/s3cmd -d $DIR -p 10 -g 100"
setenv LSFILE $ROOT/files_$DATE.ls

$BASE ls $LSFILE
$BASE move $LSFILE progress_$DATE.log
echo 'Done:', `date`

echo "\n####################\nArchiving"
python $GATK/private/python/analyzeRunReports.py archive $DIR -o $DIR.gz -D
echo 'Done:', `date`

echo "\n####################\nLoading to DB"
python $GATK/private/python/analyzeRunReports.py loadToDB $DIR.gz
echo 'Done:', `date`

# if the dir is empty we proceed
rmdir --ignore-fail-on-non-empty $DIR