/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ulb.lisa.infoh400.labs2022.services;

import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.display.SourceImage;
import com.pixelmed.network.DicomNetworkException;
import com.pixelmed.network.StorageSOPClassSCU;
import java.awt.Image;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.apache.logging.log4j.LogManager;
import ulb.lisa.infoh400.labs2022.controller.ImageJpaController;

/**
 *
 * @author Adrien Foucart
 */
public class DicomInstanceServices {
    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(DicomInstanceServices.class.getName());
    
    private File instanceFile;
    
    public DicomInstanceServices(File instanceFile){
        this.instanceFile = instanceFile;
    }
    
    public DicomInstanceServices(ulb.lisa.infoh400.labs2022.model.Image image){//doit mettre le directory complet
        //récupère l'instanceuid de l'image
        String instanceUID = image.getInstanceuid();
        this.instanceFile=new File("C:\\Users\\nasta\\INFOH400\\dcm4che-5.25.1\\bin",instanceUID);//direction où on a nos images "" puis instance
    }
    
    //on adapte cette liste d'attribut dans notre dicominstance
     public String getAttributesAsString(){
        
        AttributeList al = new AttributeList();
        try {
            al.read(instanceFile);
        } catch (IOException ex) {
            Logger.getLogger(DicomInstanceServices.class.getName()).log(Level.SEVERE, null, ex);
        } catch (DicomException ex) {
            Logger.getLogger(DicomInstanceServices.class.getName()).log(Level.SEVERE, null, ex);
        }
        String out = "";

        for( AttributeTag tag: al.keySet() ){
            out += tag + " : " + al.get(tag).getDelimitedStringValuesOrEmptyString() + "\n";
        }

        return out;
    }

    public Image getImage() {
        try {
            SourceImage dicomImg = new SourceImage(instanceFile.toString());
            return dicomImg.getBufferedImage();
        } catch (IOException | DicomException ex) {
            LOGGER.error("Couldn't get Image from instance file.", ex);
        }
        
        return null;
    }

    public void saveInstanceToDatabase() {
        try {
            AttributeList al = new AttributeList();
            al.read(instanceFile);
            
            EntityManagerFactory emfac = Persistence.createEntityManagerFactory("infoh400_PU");
            ImageJpaController imageCtrl = new ImageJpaController(emfac);
            
            String instanceUID = al.get(TagFromName.SOPInstanceUID).getSingleStringValueOrEmptyString();
            String studyUID = al.get(TagFromName.StudyInstanceUID).getSingleStringValueOrEmptyString();
            String seriesUID = al.get(TagFromName.SeriesInstanceUID).getSingleStringValueOrEmptyString();
            String patientID = al.get(TagFromName.PatientID).getSingleStringValueOrEmptyString();
            
            ulb.lisa.infoh400.labs2022.model.Image image = new ulb.lisa.infoh400.labs2022.model.Image();
            image.setInstanceuid(instanceUID);
            image.setStudyuid(studyUID);
            image.setSeriesuid(seriesUID);
            image.setPatientDicomIdentifier(patientID);
            
            imageCtrl.create(image);
            LOGGER.info("Saved instance to the database (instanceUID=" + instanceUID + ").");
        } catch (IOException | DicomException ex) {
            LOGGER.error("Couldn't save DICOM instance to the database.", ex);
        }
    }

    public void sendToStoreSCP() {
        AttributeList al = new AttributeList();//on lit le fichier avant de l'envoyer dans sopclass!
        try {
            al.read(instanceFile);
        } catch (IOException | DicomException ex) {//fait du multicatch pour dire qu'on ne fait rien avec ces erreurs
            Logger.getLogger(DicomInstanceServices.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        String instanceUID = al.get(TagFromName.SOPInstanceUID).getSingleStringValueOrEmptyString();
        String SOPClassUID = al.get(TagFromName.SOPClassUID).getSingleStringValueOrEmptyString();
        
        try {
            new StorageSOPClassSCU("localhost", 11112, "PACS", "HIS", instanceFile.getAbsolutePath(),SOPClassUID , instanceUID, 0);
        } catch (DicomNetworkException | DicomException | IOException ex) {//si problème au moment de la communication
            //donc niveau du C-store et de l'association
            Logger.getLogger(DicomInstanceServices.class.getName()).log(Level.SEVERE, null, ex);
        }
        //au moment où il essaye d'ouvrir le fichier dicom il a un soucis
        
    
    }
    
}
