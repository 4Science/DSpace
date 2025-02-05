#!/bin/bash

# Define an array of ANSI color codes for the entities
COLOR_CODES=(
    # Standard Colors
    "\e[31m"         # Red
    "\e[32m"         # Green
    "\e[33m"         # Yellow
    "\e[34m"         # Blue
    "\e[35m"         # Magenta
    "\e[36m"         # Cyan
    
    # Light/Bright Colors
    "\e[91m"         # Light Red
    "\e[92m"         # Light Green
    "\e[93m"         # Light Yellow
    "\e[94m"         # Light Blue
    "\e[95m"         # Light Magenta
    "\e[96m"         # Light Cyan
    
    # Reds & Pinks
    "\e[38;5;196m"   # Bright Red
    "\e[38;5;197m"   # Deep Pink
    "\e[38;5;198m"   # Hot Pink
    "\e[38;5;199m"   # Pink
    "\e[38;5;200m"   # Magenta Pink
    "\e[38;5;201m"   # Purple Pink
    "\e[38;5;202m"   # Orange Red
    "\e[38;5;203m"   # Salmon
    "\e[38;5;204m"   # Light Coral
    "\e[38;5;205m"   # Rose Pink
    "\e[38;5;206m"   # Orchid
    
    # Oranges
    "\e[38;5;208m"   # Orange
    "\e[38;5;209m"   # Light Orange
    "\e[38;5;210m"   # Salmon Orange
    "\e[38;5;214m"   # Gold
    "\e[38;5;215m"   # Light Gold
    "\e[38;5;216m"   # Peach
    
    # Yellows
    "\e[38;5;220m"   # Gold Yellow
    "\e[38;5;221m"   # Dark Yellow
    "\e[38;5;222m"   # Light Gold Yellow
    "\e[38;5;226m"   # Bright Yellow
    "\e[38;5;227m"   # Light Yellow
    "\e[38;5;228m"   # Pale Yellow
    
    # Greens
    "\e[38;5;34m"    # Deep Green
    "\e[38;5;40m"    # Bright Green
    "\e[38;5;46m"    # Neon Green
    "\e[38;5;47m"    # Light Bright Green
    "\e[38;5;48m"    # Turquoise Green
    "\e[38;5;70m"    # Forest Green
    "\e[38;5;71m"    # Dark Sea Green
    "\e[38;5;76m"    # Lime Green
    "\e[38;5;77m"    # Light Lime
    "\e[38;5;82m"    # Bright Lime
    "\e[38;5;83m"    # Light Green Yellow
    "\e[38;5;84m"    # Light Mint
    "\e[38;5;85m"    # Mint Green
    "\e[38;5;114m"   # Olive Green
    "\e[38;5;115m"   # Sea Green
    "\e[38;5;118m"   # Spring Green
    "\e[38;5;119m"   # Light Spring Green
    "\e[38;5;120m"   # Pale Green
    "\e[38;5;121m"   # Light Sea Green
    "\e[38;5;122m"   # Aquamarine
    "\e[38;5;123m"   # Light Aquamarine
    
    # Blues & Cyans
    "\e[38;5;25m"    # Royal Blue
    "\e[38;5;27m"    # Deep Blue
    "\e[38;5;32m"    # Ocean Blue
    "\e[38;5;33m"    # Dark Sky Blue
    "\e[38;5;38m"    # Deep Cyan
    "\e[38;5;39m"    # Sky Blue
    "\e[38;5;44m"    # Light Cyan
    "\e[38;5;45m"    # Bright Cyan
    "\e[38;5;51m"    # Neon Cyan
    "\e[38;5;74m"    # Steel Blue
    "\e[38;5;75m"    # Light Steel Blue
    "\e[38;5;81m"    # Light Blue
    "\e[38;5;87m"    # Very Light Blue
    
    # Purples & Violets
    "\e[38;5;90m"    # Dark Purple
    "\e[38;5;91m"    # Purple
    "\e[38;5;92m"    # Light Purple
    "\e[38;5;93m"    # Bright Purple
    "\e[38;5;98m"    # Light Violet
    "\e[38;5;99m"    # Violet
    "\e[38;5;105m"   # Bright Violet
    "\e[38;5;128m"   # Deep Purple
    "\e[38;5;129m"   # Bright Deep Purple
    "\e[38;5;134m"   # Medium Purple
    "\e[38;5;135m"   # Medium Violet
    "\e[38;5;141m"   # Light Medium Purple
    
    # Browns & Earth Tones
    "\e[38;5;130m"   # Dark Brown
    "\e[38;5;131m"   # Brown Red
    "\e[38;5;136m"   # Dark Gold
    "\e[38;5;137m"   # Light Brown
    "\e[38;5;138m"   # Khaki
    "\e[38;5;166m"   # Dark Orange
    "\e[38;5;172m"   # Brown Orange
    "\e[38;5;173m"   # Tan
    "\e[38;5;179m"   # Dark Khaki
    "\e[38;5;180m"   # Light Tan
)
RESET="\e[0m"
BOLD="\e[1m"

# Function to gett a random color from the array
get_random_color() {
    echo "${COLOR_CODES[$RANDOM % ${#COLOR_CODES[@]}]}"
}

# Initialize associative aray for entity color
declare -A entity_colors

# ask for CSV file location
read -p "Please enter the path to the handles CSV file: " CSV_FILE

# inspct if the file exists
if [[ ! -f "$CSV_FILE" ]]; then
    echo -e "\e[91mError: File not found at '$CSV_FILE'$RESET"
    exit 1
fi

LOG_FILE="migration_handles_execution_report.log"
> $LOG_FILE

# Read the first row (entities and submision forms) into an array to handle
IFS=',' read -r -a entity_info < "$CSV_FILE"

# Pre-assign random colors to each unique entity to easily see the difference sections
for entry in "${entity_info[@]}"; do
    IFS='|' read -r entity submission_form <<< "$entry"
    if [[ -z "${entity_colors[$entity]}" ]]; then
        # Assign a random color if this entity hasn't been seen before
        entity_colors[$entity]=$(get_random_color)
    fi
done

# Initialize commands counter and total commands executins
total_commands=$(tail -n +2 "$CSV_FILE" | awk -F',' '{for (i=1; i<=NF; i++) if ($i !~ /^[[:space:]]*$/) count++} END {print count}')
counter=0
commands=()

# Read CSV into an array
mapfile -t lines < <(tail -n +2 "$CSV_FILE")

# Display color legend
echo -e "\n${BOLD}Color Legend:$RESET"
for entity in "${!entity_colors[@]}"; do
    echo -e "${entity_colors[$entity]}$entity$RESET"
done
echo -e "----------------------------------------\n"

# Loop over each entity column
for i in "${!entity_info[@]}"; do
    IFS='|' read -r entity submission_form <<< "${entity_info[$i]}"
    
    # Get the pre-assigned color for this entity
    COLOR="${entity_colors[$entity]}"
    
    # Print sectionr with color
    echo -e "\n${COLOR}${BOLD}Processing entity: $entity with submission form: $submission_form$RESET"
    echo -e "${COLOR}----------------------------------------$RESET"
    
    # Process each handle under the column
    for line in "${lines[@]}"; do
        IFS=',' read -r -a handles <<< "$line"
        handle="${handles[$i]}"
        
        # If the handle cell is not empty or whitespace, prepare the migration command
        if [[ -n "$handle" && ! "$handle" =~ ^[[:space:]]*$ ]]; then
            ((counter++))
            cmd="./dspace migrate-entity -i $handle -n $entity -f $submission_form"
            echo -e "${COLOR}Command $counter of $total_commands: $cmd$RESET"
            commands+=("$cmd")
        fi
    done
done

# require confirmation before executing
echo -e "\n${BOLD}Review the commands above and confirm execution:$RESET"
read -p "Do you want to execute the above commands? (explicitly type yes): " confirm

if [[ "$confirm" != "yes" ]]; then
    echo -e "\e[91mAborting execution.$RESET"
    exit 0
fi

# Execute the commands with logs
counter=0
for cmd in "${commands[@]}"; do
    ((counter++))
    entity=$(echo "$cmd" | grep -o -P '(?<=-n )[^ ]+')
    COLOR="${entity_colors[$entity]}"
    
    echo -e "\n${COLOR}${BOLD}Executing command $counter of $total_commands:$RESET"
    echo -e "${COLOR}$cmd$RESET" | tee -a "$LOG_FILE"
    
    eval "$cmd" 2>&1 | tee -a "$LOG_FILE"
done
