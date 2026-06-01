package com.itc.healthtrack.utils;

import com.itc.healthtrack.models.Metric;

import java.util.List;

// utilidades clinicas compartidas entre MetricsController ReportsController y RecommendationsController
public class ClinicalUtils {

    private ClinicalUtils() {}

    // ordena las metricas de mas reciente a mas antigua
    // las entradas sin timestamp van al final
    public static void sortByTimestampDesc(List<Metric> metrics) {
        metrics.sort((a, b) -> {
            if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
            if (a.getTimestamp() == null) return 1;
            if (b.getTimestamp() == null) return -1;
            return b.getTimestamp().compareTo(a.getTimestamp());
        });
    }

    // calcula los promedios de las cinco metricas principales
    // devuelve null en los campos que no tienen datos
    // la presion solo se cuenta cuando sistolica y diastolica estan presentes juntas
    public static Averages computeAverages(List<Metric> data) {
        Averages result = new Averages();
        if (data == null || data.isEmpty()) return result;

        int sysTotal = 0, diaTotal = 0, hrTotal = 0;
        double glTotal = 0, weightTotal = 0;
        int bpCount = 0, hrCount = 0, glCount = 0, weightCount = 0;

        for (Metric m : data) {
            if (m.getSystolic() != null && m.getDiastolic() != null) {
                sysTotal += m.getSystolic();
                diaTotal += m.getDiastolic();
                bpCount++;
            }
            if (m.getHeartRate() != null) {
                hrTotal += m.getHeartRate();
                hrCount++;
            }
            if (m.getGlucoseLevel() != null) {
                glTotal += m.getGlucoseLevel();
                glCount++;
            }
            if (m.getWeight() != null) {
                weightTotal += m.getWeight();
                weightCount++;
            }
        }

        if (bpCount > 0) {
            result.systolicAvg  = sysTotal / (double) bpCount;
            result.diastolicAvg = diaTotal / (double) bpCount;
        }
        if (hrCount > 0)     result.heartRateAvg = hrTotal     / (double) hrCount;
        if (glCount > 0)     result.glucoseAvg   = glTotal     / glCount;
        if (weightCount > 0) result.weightAvg    = weightTotal / weightCount;
        return result;
    }

    // arma el texto de alertas activas a partir de la metrica mas reciente
    // evalua presion arterial glucosa frecuencia cardiaca e imc
    // la lista debe venir ordenada desc con la entrada mas reciente en el indice 0
    public static String buildAlertsText(List<Metric> history) {
        if (history == null || history.isEmpty()) {
            return "Sin métricas registradas — no se pueden calcular alertas.";
        }

        Metric latest = history.get(0);
        StringBuilder sb = new StringBuilder();

        if (latest.getSystolic() != null && latest.getDiastolic() != null) {
            int sys = latest.getSystolic();
            int dia = latest.getDiastolic();
            if (sys >= 180 || dia >= 120) {
                sb.append("• CRÍTICO — Hipertensión en crisis (")
                  .append(sys).append("/").append(dia).append(" mmHg)\n");
            } else if (sys >= 140 || dia >= 90) {
                sb.append("• ALERTA — Hipertensión (")
                  .append(sys).append("/").append(dia).append(" mmHg)\n");
            } else if (sys >= 130 || dia >= 80) {
                sb.append("• AVISO — Prehipertensión (")
                  .append(sys).append("/").append(dia).append(" mmHg)\n");
            }
        }

        if (latest.getGlucoseLevel() != null) {
            double gluc = latest.getGlucoseLevel();
            if (gluc > 300) {
                sb.append("• CRÍTICO — Glucosa extrema (").append(gluc)
                  .append(" mg/dL) — riesgo de cetoacidosis\n");
            } else if (gluc > 125) {
                sb.append("• ALERTA — Hiperglucemia (").append(gluc).append(" mg/dL)\n");
            } else if (gluc < 70) {
                sb.append("• ALERTA — Hipoglucemia (").append(gluc).append(" mg/dL)\n");
            }
        }

        if (latest.getHeartRate() != null) {
            int hr = latest.getHeartRate();
            if (hr > 120) {
                sb.append("• ALERTA — Taquicardia (").append(hr).append(" lpm)\n");
            } else if (hr < 50) {
                sb.append("• ALERTA — Bradicardia (").append(hr).append(" lpm)\n");
            }
        }

        if (latest.getBmi() != null) {
            double bmi = latest.getBmi();
            if (bmi >= 40) {
                sb.append("• ALERTA — Obesidad mórbida (IMC: ").append(bmi).append(")\n");
            } else if (bmi >= 35) {
                sb.append("• ALERTA — Obesidad severa (IMC: ").append(bmi).append(")\n");
            } else if (bmi >= 30) {
                sb.append("• AVISO — Obesidad clase I (IMC: ").append(bmi).append(")\n");
            } else if (bmi >= 25) {
                sb.append("• AVISO — Sobrepeso (IMC: ").append(bmi).append(")\n");
            } else if (bmi < 18.5) {
                sb.append("• AVISO — Bajo peso (IMC: ").append(bmi).append(")\n");
            }
        }

        if (sb.length() == 0) {
            return "No se detectaron valores fuera del rango clínico normal.";
        }
        return sb.toString().trim();
    }

    // contenedor con los cinco promedios calculados
    // un campo null significa que no habia datos suficientes para ese tipo de metrica
    public static class Averages {
        public Double systolicAvg;
        public Double diastolicAvg;
        public Double heartRateAvg;
        public Double glucoseAvg;
        public Double weightAvg;
    }
}
