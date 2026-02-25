#!/bin/bash
# Script to add @JsonIgnoreProperties(ignoreUnknown = true) to all model classes

MODEL_DIR="src/main/java/com/saraasansor/api/model"

for file in $MODEL_DIR/*.java; do
    if [ -f "$file" ]; then
        # Check if @JsonIgnoreProperties already exists
        if ! grep -q "@JsonIgnoreProperties" "$file"; then
            # Get class name from file
            CLASS_NAME=$(basename "$file" .java)
            echo "Processing $CLASS_NAME..."
            
            # Check if it's an entity class
            if grep -q "^@Entity" "$file" || grep -q "^public class" "$file"; then
                # Add import if not exists
                if ! grep -q "import com.fasterxml.jackson.annotation.JsonIgnoreProperties;" "$file"; then
                    # Find the line after package declaration
                    sed -i '' '/^package/a\
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
' "$file"
                fi
                
                # Add @JsonIgnoreProperties before class declaration
                # Find the line with @Entity or @Table or public class
                sed -i '' '/^@Entity\|^@Table\|^public class/ i\
@JsonIgnoreProperties(ignoreUnknown = true)
' "$file"
            fi
        fi
    fi
done

echo "Done!"

