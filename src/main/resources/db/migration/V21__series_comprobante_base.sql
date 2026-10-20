INSERT INTO series_comprobante (serie, ultimo_correlativo) VALUES ('B001',0),('E001',0) ON CONFLICT (serie) DO NOTHING;
