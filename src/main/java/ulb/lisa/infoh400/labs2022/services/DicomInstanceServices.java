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
import java.io.File;
import java.io.IOException;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.apache.logging.log4j.LogManager;
import ulb.lisa.infoh400.labs2022.controller.ImageJpaController;
import ulb.lisa.infoh400.labs2022.model.Image;

/**
 *
 * @author Adrien Foucart
 */
public class DicomInstanceServices {
//tout ce qui est en lien avec l'image (getImage, save dans la db etc...)
    
    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(DicomInstanceServices.class.getName());
    
    private File instanceFile;
    private Image image;
    
    public DicomInstanceServices(File instanceFile){
        this.instanceFile = instanceFile;
    }
    
    public DicomInstanceServices(Image image){
        this.image = image;
    
    //met le file dans notre pacs qui est une dossier dans notre ordi
        this.instanceFile = new File("C:\\Users\\nasta\\INFOH400\\dcm4che-5.25.1\\bin", this.image.getInstanceuid());
    }

    public java.awt.Image getDisplayableImage() {
        if( instanceFile == null ){
            return null;
        }
        try {
            SourceImage dicomImg = new SourceImage(instanceFile.toString());//pixelmed
            return dicomImg.getBufferedImage();
        } catch (IOException | DicomException ex) {
            LOGGER.error("Couldn't get Image from instance file.", ex);
        }
        
        return null;
    }
    
    public String getAttributesAsString(){
        try {
            if( instanceFile == null ){
                return "No instance file.";
            }
            
            AttributeList al = new AttributeList();
            al.read(instanceFile);
            String out = "";
            
            for( AttributeTag tag: al.keySet() ){
                out += tag + " : " + al.get(tag).getDelimitedStringValuesOrEmptyString() + "\n";
            }
            
            return out;
        } catch (IOException | DicomException ex) {
            LOGGER.error("Couldn't get instance attributes.", ex);
        }
        
        return "Couldn't get instance attributes.";
    }

    public boolean saveInstanceToDatabase() {
    //on save l'image dans notre db, pas encore dans le PACS ici ! 
        try {
            AttributeList al = new AttributeList();
            al.read(instanceFile);
    
    //lie objet imagectrl à la base de donnée
            EntityManagerFactory emfac = Persistence.createEntityManagerFactory("infoh400_PU");
            ImageJpaController imageCtrl = new ImageJpaController(emfac);
            
    //récupère les attributs liés aux colonnes de la db (info dicom)
            String instanceUID = al.get(TagFromName.SOPInstanceUID).getSingleStringValueOrEmptyString();//uid de l'image
            String studyUID = al.get(TagFromName.StudyInstanceUID).getSingleStringValueOrEmptyString();//uid des procédure
            String seriesUID = al.get(TagFromName.SeriesInstanceUID).getSingleStringValueOrEmptyString();//uid de metadata commune aux images
            String patientID = al.get(TagFromName.PatientID).getSingleStringValueOrEmptyString();//uid patient
            
            this.image = new Image();
            image.setInstanceuid(instanceUID);
            image.setStudyuid(studyUID);
            image.setSeriesuid(seriesUID);
            image.setPatientDicomIdentifier(patientID);
            
            imageCtrl.create(image);
            LOGGER.info("Saved instance to the database (instanceUID=" + instanceUID + ").");
            
            return true;
        } catch (IOException | DicomException ex) {
            LOGGER.error("Couldn't save DICOM instance to the database.", ex);
        }
        
        return false;
    }
    
    public boolean sendInstanceToSCP(){//envoie dans ce store localhost
        return sendInstanceToSCP("localhost", 11112, "STORESCP");
    }
    
    public boolean sendInstanceToSCP(String host, int port, String calledAET){
        try {
            AttributeList al = new AttributeList();
            al.read(instanceFile);
        
        //storageSOPclassSCU : classe de pixelmed : permet d'envoyer une image à storescp service via c-store
        //pas besoin de créer une instance
            new StorageSOPClassSCU("localhost", 11112, "PACS", "HIS", instanceFile.toString(), al.get(TagFromName.SOPClassUID).getDelimitedStringValuesOrEmptyString(), al.get(TagFromName.SOPInstanceUID).getDelimitedStringValuesOrEmptyString(), 0);
        //localhost = hostname (l'endroit où on envoie notre fichier)
        //port: celui du serveur
        //PACS = CalledAEtitle (nom du serveur)
        //HIS = CallingAEtitle (demande le c-store comment on s'identifie face à java)
        //instancefile = file name (là où on a l'info de notre file)
        //(TagFromName.SOPClassUID) = affected SOPClass (doit ouvrir le fichier : ce sont des choses qu'on récupère)
        //(TagFromName.SOPInstanceUID) = affectedSOPinstance (doit lire le fichier : récupère instanceuid avant de l'envoyer)
        //0 = compression (envoie en localhost)
            
            return true;
        } catch (IOException | DicomException | DicomNetworkException ex) {
            LOGGER.error("Couldn't send DICOM instance to the SCP.", ex);//si problème au moment de la communication
            //donc niveau du C-store et de l'association
        }
        
        return false;
    }
}

 

        