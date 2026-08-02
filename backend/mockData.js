const aidPlaces = [
  {
    id: "verified-hospital-1",
    name: "City Hospital",
    type: "hospital",
    icon: "+",
    distance: "1.2 km",
    eta: "5 min",
    open: "Open 24 hrs",
    verified: true,
    source: "verified_responder",
    phone: "+91 10000 00001",
    location: { lat: 28.6357, lng: 77.2221 }
  },
  {
    id: "verified-police-1",
    name: "Connaught Place Police Station",
    type: "police",
    icon: "P",
    distance: "0.8 km",
    eta: "4 min",
    open: "Open",
    verified: true,
    source: "verified_responder",
    phone: "+91 10000 00002",
    location: { lat: 28.6314, lng: 77.2167 }
  },
  {
    id: "public-repair-1",
    name: "Speedy Auto Care",
    type: "car_repair",
    icon: "R",
    distance: "0.6 km",
    eta: "3 min",
    open: "Open",
    verified: false,
    source: "public_place",
    phone: "+91 10000 00003",
    location: { lat: 28.6298, lng: 77.2232 }
  },
  {
    id: "public-fuel-1",
    name: "HP Petrol Pump",
    type: "gas_station",
    icon: "F",
    distance: "1.0 km",
    eta: "4 min",
    open: "Open 24 hrs",
    verified: false,
    source: "public_place",
    phone: "+91 10000 00004",
    location: { lat: 28.6381, lng: 77.2183 }
  },
  {
    id: "public-tire-1",
    name: "Tyre World",
    type: "tire_shop",
    icon: "T",
    distance: "1.4 km",
    eta: "7 min",
    open: "Open",
    verified: false,
    source: "public_place",
    phone: "+91 10000 00005",
    location: { lat: 28.6264, lng: 77.2148 }
  },
  {
    id: "public-pharmacy-1",
    name: "HealthPlus Pharmacy",
    type: "pharmacy",
    icon: "+",
    distance: "1.1 km",
    eta: "5 min",
    open: "Open",
    verified: false,
    source: "public_place",
    phone: "+91 10000 00006",
    location: { lat: 28.6341, lng: 77.2128 }
  },
  {
    id: "verified-fire-1",
    name: "Central Fire Station",
    type: "fire_station",
    icon: "F",
    distance: "2.2 km",
    eta: "8 min",
    open: "Open",
    verified: true,
    source: "verified_responder",
    phone: "+91 10000 00007",
    location: { lat: 28.641, lng: 77.2246 }
  },
  {
    id: "public-ev-1",
    name: "ChargeGrid EV Point",
    type: "electric_vehicle_charging_station",
    icon: "E",
    distance: "1.7 km",
    eta: "6 min",
    open: "Open",
    verified: false,
    source: "public_place",
    phone: "+91 10000 00008",
    location: { lat: 28.6248, lng: 77.2206 }
  },
  {
    id: "verified-tow-1",
    name: "FastAid Tow Partner",
    type: "towing",
    icon: "T",
    distance: "2.4 km",
    eta: "9 min",
    open: "Available",
    verified: true,
    source: "verified_responder",
    phone: "+91 10000 00009",
    location: { lat: 28.6228, lng: 77.226 }
  }
];

const incidentToTypes = {
  Accident: ["hospital", "police", "fire_station", "pharmacy"],
  Medical: ["hospital", "general_hospital", "medical_center", "medical_clinic", "doctor", "pharmacy"],
  Breakdown: ["car_repair", "tire_shop", "towing", "gas_station"],
  Fuel: ["gas_station", "electric_vehicle_charging_station", "towing"],
  Police: ["police", "hospital"],
  Fire: ["fire_station", "hospital"],
  SOS: ["hospital", "police", "fire_station"],
  Tyre: ["tire_shop"],
  Clinic: ["medical_clinic", "medical_center", "doctor"],
  Pharmacy: ["pharmacy", "drugstore"],
  Toilet: ["public_bathroom", "public_bath"],
  Rest_stop: ["rest_stop"],
  Parking: ["parking", "parking_lot", "parking_garage"],
  Medical_lab: ["medical_lab"],
  Auto_parts: ["auto_parts_store"]
};

module.exports = {
  aidPlaces,
  incidentToTypes
};
