import json
import random
from datetime import datetime, timedelta

# Crime types with their associated severity ranges
CRIME_TYPES = {
    "Assault": (6.0, 9.8),
    "Robbery": (7.0, 9.9),
    "Burglary": (4.0, 7.0),
    "Vehicle Theft": (3.0, 8.5),
    "Shoplifting": (1.5, 4.8),
    "Drugs": (2.0, 6.0),
    "Public Order": (3.0, 7.0),
    "Criminal Damage": (2.5, 6.5),
    "Weapons": (7.0, 9.5),
    "Theft From Person": (4.0, 8.0)
}

# London areas defined by approximate coordinate ranges
LONDON_AREAS = {
    "Central London": (51.50, 51.52, -0.15, -0.12),  # Higher crime density
    "Westminster": (51.49, 51.51, -0.14, -0.12),
    "Camden": (51.53, 51.55, -0.15, -0.13),
    "Islington": (51.54, 51.56, -0.11, -0.09),
    "Hackney": (51.53, 51.56, -0.07, -0.03),
    "Tower Hamlets": (51.50, 51.52, -0.04, -0.01),
    "Greenwich": (51.47, 51.49, 0.00, 0.02),
    "Lewisham": (51.44, 51.46, -0.03, 0.00),
    "Southwark": (51.49, 51.51, -0.10, -0.08),
    "Lambeth": (51.48, 51.50, -0.13, -0.11),
    "Wandsworth": (51.45, 51.47, -0.20, -0.18),
    "Hammersmith": (51.48, 51.50, -0.24, -0.22),
    "Kensington": (51.49, 51.51, -0.20, -0.18),
    "Brent": (51.55, 51.57, -0.29, -0.27),
    "Ealing": (51.50, 51.52, -0.31, -0.29),
    "Hounslow": (51.46, 51.48, -0.38, -0.35),
    "Richmond": (51.44, 51.46, -0.33, -0.31),
    "Kingston": (51.40, 51.42, -0.30, -0.27),
    "Merton": (51.40, 51.42, -0.20, -0.18),
    "Sutton": (51.35, 51.37, -0.20, -0.18),
    "Croydon": (51.36, 51.38, -0.10, -0.08),
    "Bromley": (51.40, 51.42, 0.01, 0.03),
    "Barnet": (51.62, 51.64, -0.16, -0.14),
    "Harrow": (51.58, 51.60, -0.34, -0.32),
    "Hillingdon": (51.53, 51.55, -0.48, -0.46),
    "Enfield": (51.65, 51.67, -0.09, -0.07),
    "Waltham Forest": (51.58, 51.60, -0.02, 0.00),
    "Redbridge": (51.55, 51.57, 0.07, 0.09),
    "Havering": (51.57, 51.59, 0.18, 0.20),
    "Barking": (51.54, 51.56, 0.12, 0.14),
    "Newham": (51.50, 51.52, 0.02, 0.05),
    "Bexley": (51.45, 51.47, 0.14, 0.16)
}

# Crime hotspots with higher density (add more specific areas with higher crime rates)
CRIME_HOTSPOTS = [
    # Format: (lat_min, lat_max, lon_min, lon_max, crime_multiplier)
    (51.50, 51.52, -0.15, -0.12, 3.0),  # Central London
    (51.53, 51.54, -0.14, -0.13, 2.5),  # Camden Town
    (51.48, 51.49, -0.10, -0.08, 2.5),  # Elephant & Castle
    (51.54, 51.55, -0.06, -0.04, 2.0),  # Hackney Central
    (51.52, 51.53, -0.07, -0.06, 2.0),  # Shoreditch
    (51.51, 51.52, -0.20, -0.19, 1.8),  # Notting Hill
    (51.47, 51.48, -0.17, -0.16, 1.8)   # Brixton
]

def is_in_hotspot(lat, lon):
    """Check if coordinates are in a crime hotspot and return multiplier if so"""
    for lat_min, lat_max, lon_min, lon_max, multiplier in CRIME_HOTSPOTS:
        if lat_min <= lat <= lat_max and lon_min <= lon <= lon_max:
            return multiplier
    return 1.0  # Default multiplier if not in a hotspot

def generate_crime_data(num_crimes, start_date, end_date):
    """Generate random crime data for London"""
    crimes = []
    
    # Create date range
    date_range = (end_date - start_date).days + 1
    
    for _ in range(num_crimes):
        # Select random area
        area = random.choice(list(LONDON_AREAS.keys()))
        lat_min, lat_max, lon_min, lon_max = LONDON_AREAS[area]
        
        # Generate coordinates with some randomness
        lat = random.uniform(lat_min, lat_max) + random.uniform(-0.01, 0.01)
        lon = random.uniform(lon_min, lon_max) + random.uniform(-0.01, 0.01)
        
        # Check if in hotspot
        hotspot_multiplier = is_in_hotspot(lat, lon)
        
        # Generate random date within range
        random_day = random.randint(0, date_range - 1)
        crime_date = start_date + timedelta(days=random_day)
        date_str = crime_date.strftime("%Y-%m-%d")
        
        # Select crime type with weighted probabilities
        # More common crimes should have higher weights
        crime_type = random.choices(
            list(CRIME_TYPES.keys()),
            weights=[25, 20, 18, 15, 10, 5, 3, 2, 1, 1],  # Adjust weights as needed
            k=1
        )[0]
        
        # Generate severity based on crime type
        min_severity, max_severity = CRIME_TYPES[crime_type]
        
        # Apply time-based modifiers - night time crimes are more severe
        hour_of_day = random.randint(0, 23)
        time_multiplier = 1.0
        if hour_of_day >= 22 or hour_of_day <= 4:
            time_multiplier = 1.2  # Night crimes are more severe
        
        # Calculate final severity with hotspot modifier
        severity = round(random.uniform(min_severity, max_severity) * hotspot_multiplier * time_multiplier, 1)
        severity = min(9.9, severity)  # Cap at 9.9
        
        # Create crime entry
        crime = {
            "latitude": round(lat, 7),
            "longitude": round(lon, 7),
            "severity": severity,
            "date": date_str,
            "type": crime_type
        }
        
        crimes.append(crime)
    
    return crimes

def main():
    # Set date range (30 days up to May 21, 2025)
    end_date = datetime(2025, 5, 21)
    start_date = end_date - timedelta(days=30)
    
    # Generate 3000 crimes for the 30-day period
    # This is a high density to ensure good coverage
    crimes = generate_crime_data(3000, start_date, end_date)
    
    # Save to JSON file
    with open('new_crime_data.json', 'w') as f:
        json.dump(crimes, f, indent=2)
    
    print(f"Generated {len(crimes)} crime reports from {start_date.strftime('%Y-%m-%d')} to {end_date.strftime('%Y-%m-%d')}")
    print("Data saved to new_crime_data.json")

if __name__ == "__main__":
    main() 