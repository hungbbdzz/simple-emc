$recipes = [ordered]@{
    dragonfire_scepter = [ordered]@{
        Pattern = @(' BD', ' CF', 'S  ')
        Keys = [ordered]@{ B = 'minecraft:dragon_breath'; D = 'minecraft:dragon_head'; C = 'velorise_emc:wireless_core'; F = 'minecraft:fire_charge'; S = 'minecraft:stick' }
    }
    resonant_echo = [ordered]@{
        Pattern = @(' ER', ' CE', 'S  ')
        Keys = [ordered]@{ E = 'minecraft:echo_shard'; R = 'minecraft:recovery_compass'; C = 'velorise_emc:wireless_core'; S = 'minecraft:stick' }
    }
    inferno_rod = [ordered]@{
        Pattern = @(' RR', ' CR', 'F  ')
        Keys = [ordered]@{ R = 'minecraft:blaze_rod'; C = 'velorise_emc:wireless_core'; F = 'minecraft:fire_charge' }
    }
    ghastfire_staff = [ordered]@{
        Pattern = @(' FG', ' CP', 'S  ')
        Keys = [ordered]@{ F = 'minecraft:fire_charge'; G = 'minecraft:ghast_tear'; C = 'velorise_emc:wireless_core'; P = 'minecraft:gunpowder'; S = 'minecraft:stick' }
    }
    wither_scepter = [ordered]@{
        Pattern = @(' HH', ' CH', 'S  ')
        Keys = [ordered]@{ H = 'minecraft:wither_skeleton_skull'; C = 'velorise_emc:wireless_core'; S = 'minecraft:stick' }
    }
    shulker_seeker = [ordered]@{
        Pattern = @(' EK', ' CP', 'S  ')
        Keys = [ordered]@{ E = 'minecraft:end_rod'; K = 'minecraft:shulker_shell'; C = 'velorise_emc:wireless_core'; P = 'minecraft:popped_chorus_fruit'; S = 'minecraft:stick' }
    }
    guardian_lens = [ordered]@{
        Pattern = @(' PH', ' CN', 'S  ')
        Keys = [ordered]@{ P = 'minecraft:prismarine_crystals'; H = 'minecraft:heart_of_the_sea'; C = 'velorise_emc:wireless_core'; N = 'minecraft:nautilus_shell'; S = 'minecraft:stick' }
    }
    evoker_fang_staff = [ordered]@{
        Pattern = @(' BT', ' CE', 'S  ')
        Keys = [ordered]@{ B = 'minecraft:bone_block'; T = 'minecraft:totem_of_undying'; C = 'velorise_emc:wireless_core'; E = 'minecraft:emerald_block'; S = 'minecraft:stick' }
    }
    stormcaller = [ordered]@{
        Pattern = @(' LT', ' CK', 'S  ')
        Keys = [ordered]@{ L = 'minecraft:lightning_rod'; T = 'minecraft:trident'; C = 'velorise_emc:wireless_core'; K = 'minecraft:copper_block'; S = 'minecraft:stick' }
    }
    frostbolt_wand = [ordered]@{
        Pattern = @(' NB', ' CA', 'S  ')
        Keys = [ordered]@{ N = 'minecraft:snowball'; B = 'minecraft:blue_ice'; C = 'velorise_emc:wireless_core'; A = 'minecraft:amethyst_shard'; S = 'minecraft:stick' }
    }
    webcaster = [ordered]@{
        Pattern = @(' WF', ' CW', 'S  ')
        Keys = [ordered]@{ W = 'minecraft:cobweb'; F = 'minecraft:fermented_spider_eye'; C = 'velorise_emc:wireless_core'; S = 'minecraft:stick' }
    }
}

$targets = @(
    @{ Directory = Join-Path $PSScriptRoot '..\forge-1.20.1\src\main\resources\data\velorise_emc\recipes'; ResultKey = 'item' },
    @{ Directory = Join-Path $PSScriptRoot '..\neoforge-1.21.1\src\main\resources\data\velorise_emc\recipe'; ResultKey = 'id' }
)

foreach ($target in $targets) {
    foreach ($entry in $recipes.GetEnumerator()) {
        $result = [ordered]@{ count = 1 }
        $result[$target.ResultKey] = 'velorise_emc:' + $entry.Key
        $key = [ordered]@{}
        foreach ($ingredient in $entry.Value.Keys.GetEnumerator()) {
            $key[$ingredient.Key] = [ordered]@{ item = $ingredient.Value }
        }
        $recipe = [ordered]@{
            type = 'minecraft:crafting_shaped'
            category = 'equipment'
            pattern = $entry.Value.Pattern
            key = $key
            result = $result
        }
        $path = Join-Path $target.Directory ($entry.Key + '.json')
        $json = $recipe | ConvertTo-Json -Depth 8
        [System.IO.File]::WriteAllText($path, $json + [Environment]::NewLine,
            [System.Text.UTF8Encoding]::new($false))
    }
}
