import json

# Load the existing crime data
with open('app/src/main/res/raw/crime_data.json', 'r') as f:
    existing_data = json.load(f)

# Load the new crime data
with open('new_crime_data.json', 'r') as f:
    new_data = json.load(f)

# Check how many records we have
print(f"Existing crime records: {len(existing_data)}")
print(f"New crime records: {len(new_data)}")

# Add region field to new data (which might be missing) using a simple approximation
for crime in new_data:
    # Approximate London region based on coordinates
    # This is a simplification - production code would use proper geocoding
    lat, lon = crime["latitude"], crime["longitude"]
    
    # Simple region mapping based on coordinates
    if 51.53 <= lat <= 51.57 and -0.15 <= lon <= -0.08:
        crime["region"] = "Camden"
    elif 51.49 <= lat <= 51.52 and -0.15 <= lon <= -0.12:
        crime["region"] = "Westminster"
    elif 51.53 <= lat <= 51.56 and -0.10 <= lon <= -0.03:
        crime["region"] = "Hackney"
    elif 51.50 <= lat <= 51.53 and -0.05 <= lon <= 0.00:
        crime["region"] = "Tower Hamlets"
    elif 51.46 <= lat <= 51.49 and -0.01 <= lon <= 0.04:
        crime["region"] = "Greenwich"
    elif 51.48 <= lat <= 51.51 and -0.11 <= lon <= -0.06:
        crime["region"] = "Southwark"
    elif 51.47 <= lat <= 51.50 and -0.14 <= lon <= -0.10:
        crime["region"] = "Lambeth"
    elif 51.50 <= lat <= 51.53 and -0.20 <= lon <= -0.15:
        crime["region"] = "Kensington"
    elif 51.52 <= lat <= 51.54 and -0.08 <= lon <= -0.05:
        crime["region"] = "Shoreditch"
    else:
        crime["region"] = "Greater London"

# Merge the data
merged_data = existing_data + new_data

# Sort by date (recent first)
merged_data.sort(key=lambda x: x["date"], reverse=True)

# Save the combined data
with open('app/src/main/res/raw/crime_data_updated.json', 'w') as f:
    json.dump(merged_data, f, indent=2)

print(f"Total crime records after merge: {len(merged_data)}")
print("Updated crime data saved to app/src/main/res/raw/crime_data_updated.json")

# Also save a backup of original data
with open('app/src/main/res/raw/crime_data_original_backup.json', 'w') as f:
    json.dump(existing_data, f, indent=2)
print("Original data backed up to app/src/main/res/raw/crime_data_original_backup.json") 