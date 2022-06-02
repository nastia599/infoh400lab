/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ulb.lisa.infoh400.labs2022.services;

import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.DicomDirectory;
import com.pixelmed.dicom.DicomDirectoryRecord;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.TagFromName;
import java.io.File;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Adrien Foucart
 */
public class DicomDirectoryServices { //récupère tout ce qui est en lien avec pixelmed
//donne les opérations dicom qu'on peut faire avec le file: celui-ci est lié au file
//c'est via cette classe qu'on peut stocker par exemple
 
    private static final Logger LOGGER = LogManager.getLogger(DicomDirectoryServices.class.getName());
    
    private DicomDirectory ddr;
    private DicomDirectoryRecord selectedRecord;
    
    public DicomDirectoryServices(String path){
        readDicomDirectory(new File(path));
    }
    
    public DicomDirectoryServices(File f){
        readDicomDirectory(f);
    }
    
    private void readDicomDirectory(File f){
        //spécifique à pixelmed
        try {
        //permet de lire un fichier et de l'extraire sous forme info dicom (tag etc.. selon standard dicom)
            AttributeList list = new AttributeList();

            list.read(new DicomInputStream(f));
            ddr = new DicomDirectory(list);//implémente treemodel dans le standard
        } catch (IOException | DicomException ex) {//si problème de permission etc...
            LOGGER.error("Couldn't read DICOM Directory", ex);
            ddr = null;
        }
    }

    public DicomDirectory getModel() {
        return ddr;
    }
    
    public void setSelectedRecord(Object o){
        selectedRecord = (DicomDirectoryRecord) o;//select le file
    }
    
    public String getSelectedRecordAttributes(){
    //donne au constructeur le fichier et avec attribute list: pour le read
        if( selectedRecord == null ){
            return "No selected record";
        }
        
        AttributeList al = selectedRecord.getAttributeList();
        String out = "";

        for( AttributeTag tag: al.keySet() ){
            out += tag + " : " + al.get(tag).getDelimitedStringValuesOrEmptyString() + "\n";
        }

        return out;
    }
    
    public boolean selectedRecordIsImage(){
        if( selectedRecord == null ){
            return false;
        }
        
        AttributeList al = selectedRecord.getAttributeList();
        String recordType = al.get(TagFromName.DirectoryRecordType).getSingleStringValueOrEmptyString();
        
        return recordType.equalsIgnoreCase("IMAGE");
    }
    
    public File getSelectedRecordFile(String selectedDirectory){
        if( selectedRecord == null ){
            return null;
        }
        
        AttributeList al = selectedRecord.getAttributeList();
        
        String relativePath = al.get(TagFromName.ReferencedFileID).getDelimitedStringValuesOrEmptyString();
        return new File(selectedDirectory, relativePath);
    }
    
}
