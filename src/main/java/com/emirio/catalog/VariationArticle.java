package com.emirio.catalog;

import jakarta.persistence.*;

@Entity
@Table(
    name = "variation_article",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"article_id", "couleur_id", "taille_id"})
    }
)
public class VariationArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "couleur_id", nullable = false)
    private Color couleur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "taille_id", nullable = false)
    private Size taille;

    @Column(name = "prix", nullable = false)
    private double prix;

    @Column(name = "quantite_stock", nullable = false)
    private int quantiteStock;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image_data_1", columnDefinition = "MEDIUMBLOB")
    private byte[] imageData1;

    @Column(name = "image_name_1")
    private String imageName1;

    @Column(name = "image_type_1")
    private String imageType1;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image_data_2", columnDefinition = "MEDIUMBLOB")
    private byte[] imageData2;

    @Column(name = "image_name_2")
    private String imageName2;

    @Column(name = "image_type_2")
    private String imageType2;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image_data_3", columnDefinition = "MEDIUMBLOB")
    private byte[] imageData3;

    @Column(name = "image_name_3")
    private String imageName3;

    @Column(name = "image_type_3")
    private String imageType3;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image_data_4", columnDefinition = "MEDIUMBLOB")
    private byte[] imageData4;

    @Column(name = "image_name_4")
    private String imageName4;

    @Column(name = "image_type_4")
    private String imageType4;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "model_3d_data", columnDefinition = "LONGBLOB")
    private byte[] model3dData;

    @Column(name = "model_3d_name")
    private String model3dName;

    @Column(name = "model_3d_type")
    private String model3dType;

    public Long getId() {
        return id;
    }

    public Article getArticle() {
        return article;
    }

    public void setArticle(Article article) {
        this.article = article;
    }

    public Color getCouleur() {
        return couleur;
    }

    public void setCouleur(Color couleur) {
        this.couleur = couleur;
    }

    public Size getTaille() {
        return taille;
    }

    public void setTaille(Size taille) {
        this.taille = taille;
    }

    public double getPrix() {
        return prix;
    }

    public void setPrix(double prix) {
        this.prix = prix;
    }

    public int getQuantiteStock() {
        return quantiteStock;
    }

    public void setQuantiteStock(int quantiteStock) {
        this.quantiteStock = quantiteStock;
    }

    public byte[] getImageData1() {
        return imageData1;
    }

    public void setImageData1(byte[] imageData1) {
        this.imageData1 = imageData1;
    }

    public String getImageName1() {
        return imageName1;
    }

    public void setImageName1(String imageName1) {
        this.imageName1 = imageName1;
    }

    public String getImageType1() {
        return imageType1;
    }

    public void setImageType1(String imageType1) {
        this.imageType1 = imageType1;
    }

    public byte[] getImageData2() {
        return imageData2;
    }

    public void setImageData2(byte[] imageData2) {
        this.imageData2 = imageData2;
    }

    public String getImageName2() {
        return imageName2;
    }

    public void setImageName2(String imageName2) {
        this.imageName2 = imageName2;
    }

    public String getImageType2() {
        return imageType2;
    }

    public void setImageType2(String imageType2) {
        this.imageType2 = imageType2;
    }

    public byte[] getImageData3() {
        return imageData3;
    }

    public void setImageData3(byte[] imageData3) {
        this.imageData3 = imageData3;
    }

    public String getImageName3() {
        return imageName3;
    }

    public void setImageName3(String imageName3) {
        this.imageName3 = imageName3;
    }

    public String getImageType3() {
        return imageType3;
    }

    public void setImageType3(String imageType3) {
        this.imageType3 = imageType3;
    }

    public byte[] getImageData4() {
        return imageData4;
    }

    public void setImageData4(byte[] imageData4) {
        this.imageData4 = imageData4;
    }

    public String getImageName4() {
        return imageName4;
    }

    public void setImageName4(String imageName4) {
        this.imageName4 = imageName4;
    }

    public String getImageType4() {
        return imageType4;
    }

    public void setImageType4(String imageType4) {
        this.imageType4 = imageType4;
    }

    public byte[] getModel3dData() {
        return model3dData;
    }

    public void setModel3dData(byte[] model3dData) {
        this.model3dData = model3dData;
    }

    public String getModel3dName() {
        return model3dName;
    }

    public void setModel3dName(String model3dName) {
        this.model3dName = model3dName;
    }

    public String getModel3dType() {
        return model3dType;
    }

    public void setModel3dType(String model3dType) {
        this.model3dType = model3dType;
    }}