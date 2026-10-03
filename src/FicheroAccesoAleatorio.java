import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.AbstractMap.SimpleEntry;

public class FicheroAccesoAleatorio {

    private File f;
    private List<Map.Entry<String, Integer>> campos;
    private long longReg;
    private long numReg = 0;

    public FicheroAccesoAleatorio(String nomFich, List<Map.Entry<String, Integer>> campos) throws IOException {

        this.campos = campos;
        this.f = new File(nomFich);
        longReg = 0;

        for (Map.Entry<String, Integer> campo : campos) {
            this.longReg += campo.getValue();
        }

        if (f.exists()) {
            this.numReg = f.length() / this.longReg;
        }
    }

    public long getNumReg() { return numReg; }

    public void insertar(Map<String, String> reg) throws IOException {

        insertar(reg, this.numReg++);
    }

    public void insertar(Map<String, String> reg, long pos) throws IOException {

        try (RandomAccessFile faa = new RandomAccessFile(f, "rws")) {
            faa.seek(pos * longReg);

            for (Map.Entry<String, Integer> campo : campos) {
                String nomCampo = campo.getKey();
                Integer longCampo = campo.getValue();
                String valorCampo = reg.get(nomCampo);
                if (valorCampo == null) { valorCampo = ""; }
                String valorCampoForm = String.format("%1$-" + longCampo + "s", valorCampo);
                faa.write(valorCampoForm.getBytes("UTF-8"), 0, longCampo);
            }
        }
    }

    public static void main(String[] args) {

        List<Map.Entry<String, Integer>> campos = new ArrayList<>();
        campos.add(new SimpleEntry<>("DNI", 9));
        campos.add(new SimpleEntry<>("NOMBRE", 32));
        campos.add(new SimpleEntry<>("CP", 5));

        FicheroAccesoAleatorio faa = null;

        try {
            faa = new FicheroAccesoAleatorio("fic_acceso_aleat.dat", campos);

            Map<String, String> reg = new HashMap<>();
            reg.put("DNI", "56789012B"); reg.put("NOMBRE", "SAMPER"); reg.put("CP", "29720");
            faa.insertar(reg);
            reg.clear();

            reg.put("DNI", "89012345E"); reg.put("NOMBRE", "ROJAS");
            faa.insertar(reg);
            reg.clear();

            reg.put("DNI", "23456789D"); reg.put("NOMBRE", "DORCE"); reg.put("CP", "13700");
            faa.insertar(reg);
            reg.clear();

            reg.put("DNI", "78901234X"); reg.put("NOMBRE", "NADALES"); reg.put("CP", "44126");
            faa.insertar(reg, 7); // Overwrites record at position 1
        }
        catch (IOException e) {
            System.err.printf("E/S Error: %s\n", e.getMessage());
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        try {
            faa.ObtenerValorCampo(0, "DNI");
            faa.ObtenerValorCampo(1, "NOMBRE");
            faa.ObtenerValorCampo(2, "CP");

            faa.ObtenerValorCampo(3, "DNI");
            faa.ObtenerValorCampo(14, "NOMBRE");
            faa.ObtenerValorCampo(0, "TELEFONO");
        }
        catch(IllegalArgumentException e){
            System.err.println("Error: " + e.getMessage());
        }
        catch(Exception e){
            System.err.println("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Metodo que sirve para mostrar el valor del campo buscado
     * @param pos la posicion en bytes del registro buscado en el archivo
     * @param nombreCampo nomenclatura del campo que se desea devolver su valor
     * @throws IllegalArgumentException saltará si no se encuentra el campo indicado
     */
    public void ObtenerValorCampo(long pos, String nombreCampo) throws IllegalArgumentException {

        long offset = 0;
        int campoLong = 0;
        boolean campoEncontrado = false;

        //Foreach de los campos para buscar el indicado y guardarnos el valor de bytes en el fichero
        for(Map.Entry<String, Integer> campo : campos){

            if(campo.getKey().equals(nombreCampo)){
                campoLong = campo.getValue();
                campoEncontrado = true;
                break;
            }
            //suma el valor de los campos anteriores
            offset += campo.getValue();
        }

        //si el campo indicado no coincide con ninguno salta la excepción
        if(!campoEncontrado){
            throw new IllegalArgumentException("Field " + nombreCampo + " not found");
        }

        //abrimos el archivo
        try(RandomAccessFile raf = new RandomAccessFile(this.f, "r")){

            /*la posicion que necesitamos encontrar la hallamos multiplicando la posicion buscada por el tamaño de cada registro
            y sumandole el tamaño de los campos anteriores al buscado*/
            long posicion = pos * longReg + offset;

            //si la posicion y el tamaño del campo buscado sorpasan el tamaño del archivo salta excepción
            if(posicion + campoLong > f.length()){
                throw new FieldNotFoundException("Asked for a position longer  than the selected File");
            }

            //posicioneamos el archivo en la posicion marcada
            raf.seek(posicion);

            //creamos un buffer con los bytes del campo
            byte[] buffer = new byte[campoLong];
            //leemos el archivo con esos bytes y llenamos el array
            raf.readFully(buffer);

            //mostramos el resultado por consola
            System.out.println();
            System.out.println("The " + nombreCampo + " of register number " +  (pos + 1) + " is " + new String(buffer, "UTF-8").trim());

        }
        catch(IOException e){
            System.err.println(e.getMessage());
        }
        catch(FieldNotFoundException e){
            System.err.println("Field " + nombreCampo + " not found");
        }
        catch(Exception e){
            System.err.println("Unexpected Error: " + e.getMessage());
        }
    }
}
