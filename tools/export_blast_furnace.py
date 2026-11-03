"""Export the approved blast furnace rig, independent of ordinary machine footprints."""
from export_machines import convert
convert('blast_furnace','Blast-Furnace.bbmodel',('Blast Furnace','Доменная печь'),('assembled_idle','processing','',''),rig_only=True)
