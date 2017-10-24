package Recursos;

/**
 *
 * @author Jônatas Trabuco Belotti [jonatas.t.belotti@hotmail.com]
 */
public class Numero {

  public static double parseDouble(String valor) {
    String numeros;
    double resposta;
    
    numeros = "";
    
    for (char c : valor.toCharArray()) {
      if (c == '0' || c == '1' || c == '2' || c == '3' || c == '4' || c == '5' || c == '6' || c == '7' || c == '8' || c == '9' || c == '.' || c == ',' || c == '-') {
        numeros += c;
      }
    }

    try {
      numeros = numeros.replaceAll(",", ".");
      resposta = Double.parseDouble(numeros);
    } catch (Exception e) {
      resposta = 0D;
    }

    return resposta;
  }

}
