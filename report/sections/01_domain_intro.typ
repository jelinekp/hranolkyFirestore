= Application Domain & System Overview

This report documents the refactoring of the *Beams and Jointers* (_Hranolky a Spárovky_) warehouse management system, comprising an Android terminal application and a React web administration interface. The system manages inventory data for a woodworking company producing timber beams and jointer boards.

== Problem Domain

#grid(
  columns: (1fr, 40%),
  column-gutter: 1.5em,
  [
    "Hranolky" (timber beams) are intermediate products used to manufacture "spárovky" (jointer boards), which are then used to produce solid wood furniture. The beams are cut from planks in a way that optimizes their placement into jointer boards. Because all wood defects such as cracks and knots must be removed, beams of various dimensions (length, width, thickness), wood species, and qualities are produced.

    The existing ERP system only allows storing warehouse data in cubic meters with insufficient precision (maximum two decimal places). Additionally, the ERP system is slow and inflexible, so data is stored in aggregate form for a given wood species and quality. Individual item dimensions are not tracked.

    To improve the utilization of warehouse materials, precise quantities of each unique beam type need to be known. Material quantities were previously calculated manually, only during quarterly inventories and annual audits.
  ],
  [
    #v(0.5em)
    #figure(
      image("../media/sklad_hranolku.jpeg", width: 100%),
      caption: [Warehouse storing wooden "hranolky" (timber beams)],
    ) <fig-warehouse>
  ],
)

#figure(
  image("../media/sparovka.jpeg", width: 80%),
  caption: [Jointer board ("spárovka") composed of individual timber beams],
) <fig-jointer>

== System Components

The solution consists of two primary applications working in tandem with a shared Firebase backend.

=== 1. Android Terminal Application (Data Entry)

#grid(
  columns: (1fr, 35%),
  column-gutter: 1.5em,
  [
    Since Zebra terminals are used at other company workplaces, they were chosen for recording material receipts and dispatches from this warehouse. The terminals have an integrated infrared barcode and QR code reader, enabling instant and reliable code scanning.

    QR codes were deployed throughout the warehouse (252 locations), encoding product identifiers including wood species, quality grade, and dimensions.

    The Android app is responsible for:
    - Scanning QR codes to load warehouse slots
    - Recording daily material movements (receipts/dispatches)
    - Creating new inventory items automatically from QR data
    - Performing inventory checks
  ],
  [
    #figure(
      image("../media/warehouse_zebra.png", width: 100%),
      caption: [Zebra TC200J terminal with integrated infrared QR code reader],
    ) <fig-zebra>
  ],
)

#figure(
  image("../media/label_captioned.png", width: 70%),
  caption: [QR code label at a storage location],
) <fig-qr-label>

=== 2. React Web Application (Management & Reporting)

The web application serves as the administrative interface for warehouse managers.
Technically, it is a Single Page Application (SPA) built with React and TypeScript
but it has multiple screens.

The web app is responsible for:
- Viewing real-time inventory levels
- Filtering and sorting warehouse slots by dimensions and quality
- Administrative tasks (managing authorized devices)
- Generating inventory reports and CSV exports
- Visualizing stock history trends

#figure(
  image("../media/web_app.png", width: 100%),
  caption: [Web application dashboard showing inventory overview],
) <fig-web-app>
