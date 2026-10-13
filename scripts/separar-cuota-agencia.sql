USE stockflow_db;
ALTER TABLE salidas MODIFY COLUMN monto_recaudado DECIMAL(10,2) NULL;
SELECT COUNT(*) AS salidas_historicas, SUM(monto_recaudado) AS recaudacion_conservada FROM salidas;