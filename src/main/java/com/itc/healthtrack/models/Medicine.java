package com.itc.healthtrack.models;

// medicamento del catalogo guardado en la coleccion medicines
// los medicamentos retirados nunca se eliminan sino que se desactivan con active false
// esto preserva la integridad del historial de tratamientos anteriores
public class Medicine {

    private String id;
    private String genericName;
    private String commercialName;
    private String manufacturer;
    private boolean active;

    // constructor vacio requerido por el sdk de firestore
    public Medicine() {}

    public Medicine(String id, String genericName, String commercialName,
                    String manufacturer, boolean active) {
        this.id = id;
        this.genericName = genericName;
        this.commercialName = commercialName;
        this.manufacturer = manufacturer;
        this.active = active;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public String getCommercialName() { return commercialName; }
    public void setCommercialName(String commercialName) { this.commercialName = commercialName; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    // texto que aparece en el ComboBox del medico al seleccionar un medicamento para recetar
    @Override
    public String toString() {
        if (commercialName != null && !commercialName.isBlank()) {
            return genericName + " (" + commercialName + ")";
        }
        return genericName;
    }
}
