package agh.ics.oop.model;

import agh.ics.oop.model.util.MapVisualizer;

import java.util.*;

public class DefaultWorldMap implements WorldMap {
    protected Map<Vector2d, AnimalList> animals = new HashMap<>();
    protected Map<Vector2d, Plant> plants = new HashMap<>();

    protected List<Animal> deadAnimals;
    protected List<Animal> livingAnimals;

    protected Map<Genome, Integer> genomeCount = new HashMap<>();

    private final List<MapChangeListener> observers = new LinkedList<>();

    public void addObserver(MapChangeListener newObserver) {
        observers.add(newObserver);
    }

    public void removeObserver(MapChangeListener toRemove) {
        observers.remove(toRemove);
    }

    public void mapChanged(String s) {
        for (MapChangeListener observer : observers)
            observer.mapChanged(this, s);
    }

    protected UUID mapId;

    protected final int width;
    protected final int height;

    protected final int minEquatorHeight;

    protected final int maxEquatorHeight;

    protected int freeEquatorFields;
    protected int takenEquatorFields;

    protected int freeNonEquatorFields;
    protected int takenNonEquatorFields;
    private final Settings settings;


    public DefaultWorldMap(Settings settings) {
        this.mapId = UUID.randomUUID();
        this.width = settings.getMapWidth();
        this.height = settings.getMapHeight();

        this.settings = settings;
        this.livingAnimals = new ArrayList<>();
        this.deadAnimals = new ArrayList<>();

        this.minEquatorHeight = (int) (0.4 * height);
        this.maxEquatorHeight = (int) (0.6 * height);

        this.freeEquatorFields = width * (maxEquatorHeight - minEquatorHeight);
        this.takenEquatorFields = 0;

        this.freeNonEquatorFields = (width * height) - freeEquatorFields;
        this.takenNonEquatorFields = 0;
    }


    @Override
    public void place(Animal animal) {
        Vector2d animalPos = animal.getPosition();
        if (animals.get(animalPos) != null) {
            animals.get(animalPos).add(animal);
        } else {
            AnimalList newAnimalList = new AnimalList();
            newAnimalList.add(animal);
            animals.put(animalPos, newAnimalList);
        }

        livingAnimals.add(animal);
        animal.setMapWhereItLives(this);
        addGenomeToStats(animal.getGenome());

        mapChanged("Animal " + animal + " placed at " + animalPos + ".");
    }

    public void growPlant(Vector2d position) {
        Plant plant = new Plant(position);
        plants.put(position, plant);
    }

    public void growPlantMassive() {
        int howManyPlantsToGrow = settings.getNewPlantsPerDay();
        growPlantsInRandomFields(howManyPlantsToGrow);

    }

    public boolean isEquatorOvergrown() {
        for (int i = minEquatorHeight; i < maxEquatorHeight; i++) {
            for (int j = 0; j < width; j++) {
                Vector2d fieldToCheck = new Vector2d(j, i);
                if (plants.get(fieldToCheck) == null) return false;
            }
        }
        return true;
    }

    public void growPlantEquator() {
        boolean growthSuccessful = false;
        while (!growthSuccessful) {
            Vector2d currRandPos = randomFieldEquator();
            if (plants.get(currRandPos) == null) {
                growPlant(currRandPos);
                freeEquatorFields--;
                takenEquatorFields++;
                growthSuccessful = true;
            }
        }
    }

    public void growPlantNonEquator() {
        boolean growthSuccessful = false;
        while (!growthSuccessful) {
            Vector2d currRandPos = randomFieldNonEquator();
            if (plants.get(currRandPos) == null) {
                growPlant(currRandPos);
                freeNonEquatorFields--;
                takenNonEquatorFields++;
                growthSuccessful = true;
            }
        }
    }

    public void growPlantsInRandomFields(int plantCnt) {

        Random rand = new Random();
        int growthLocationResult = rand.nextInt(4);

        for (int i = 0; i < plantCnt; i++) {
            if ((growthLocationResult > 0 || freeNonEquatorFields == 0) && freeEquatorFields > 0) {
                // grow within equator
                growPlantEquator();
            } else if (freeNonEquatorFields > 0) {
                // grow within normal fields
                growPlantNonEquator();
            } else {
                // don't grow
                break;
            }
        }


    }

    @Override
    public void moveForward(Animal animal) {
        Vector2d currPos = animal.getPosition();
        animal.moveForward(this);
        Vector2d nextPos = animal.getPosition();

        if (!currPos.equals(nextPos)) {
            AnimalList currPosAnimalList = animals.get(currPos);

            // remove animal from the list or remove the entire list if it's the only animal
            if (currPosAnimalList != null) {
                if (currPosAnimalList.size() == 1) {
                    animals.remove(currPos);
                } else {
                    currPosAnimalList.remove(animal);
                }
            }

            // add animal to an existing list on nextPos or create a new list
            if (animals.get(nextPos) == null) {
                AnimalList newAnimalList = new AnimalList();
                newAnimalList.add(animal);
                animals.put(nextPos, newAnimalList);
            } else {
                AnimalList nextAnimalList = animals.get(nextPos);
                nextAnimalList.add(animal);
            }
        }

        mapChanged("Animal " + animal + " moved forward: " + currPos + " -> " + nextPos + ".");
//        System.out.println("Animal " + animal + " moved forward " + currPos + " -> " + nextPos);
    }

    @Override
    public void turn(Animal animal, int timesTurned) {
        MapDirection orientationOld = animal.getOrientation();
        animal.turn(timesTurned);
        MapDirection orientationNew = animal.getOrientation();
        mapChanged("Animal " + animal + " turned: " + orientationOld + " -> " + orientationNew + ".");
    }

    @Override
    public boolean isOccupied(Vector2d position) {
        return animals.get(position) != null || plants.get(position) != null;
    }

    @Override
    public WorldElement objectAt(Vector2d position) {
        if (animals.get(position) != null)
            return animals.get(position);
        return plants.get(position);
    }

    @Override
    public List<WorldElement> getElements() {
        List<WorldElement> allElements = new ArrayList<>();
        for (Map.Entry<Vector2d, AnimalList> entry : animals.entrySet()) {
            WorldElement currElement = entry.getValue();
            allElements.add(currElement);
        }
        return allElements;
    }

    @Override
    public UUID getId() {
        return this.mapId;
    }

    @Override
    public boolean canMoveTo(Vector2d position) {
        return (position.getX() >= 0
                && position.getX() < width
                && position.getY() >= 0
                && position.getY() < height
        );
    }

    @Override
    public String toString() {
        MapVisualizer toVisualize = new MapVisualizer(this);
        return toVisualize.draw(new Vector2d(0, 0), new Vector2d(width - 1, height - 1));
    }

    public List<Animal> createCurrAnimalList() {
        return livingAnimals;
    }

    public void moveAnimalByGene(Animal animal, int dayNo) {
        int geneNo = (animal.getStartGeneId() + dayNo) % settings.getGenomeLength();
        int turnVal = animal.getGenes()[geneNo];
        turn(animal, turnVal);
        moveForward(animal);
    }

    public void moveAllAnimalsByGene(int dayNo) {
        List<Animal> animalsToMove = createCurrAnimalList();
        for (Animal currAnimal : animalsToMove) {
            moveAnimalByGene(currAnimal, dayNo);
        }
    }

    public void changeOneAnimalsEnergy(Animal animal, int dEnergy) {
        animal.changeEnergy(dEnergy);
    }

    public void changeAllAnimalsEnergy(int dEnergy) {
        List<Animal> animalsToChange = createCurrAnimalList();
        for (Animal currAnimal : animalsToChange) {
            changeOneAnimalsEnergy(currAnimal, dEnergy);
        }
    }

    public void removeDeadAnimals(int dayNo) {
        Iterator<Animal> iterator = livingAnimals.iterator();

        while (iterator.hasNext()) {
            Animal currAnimal = iterator.next();
            if (!currAnimal.isAlive()) {
                currAnimal.setDayOfDeath(dayNo);
                iterator.remove();
                deadAnimals.add(currAnimal);
                removeGenomeFromStats(currAnimal.getGenome());


                // animal removal procedure
                Vector2d currPos = currAnimal.getPosition();
                AnimalList currPosAnimalList = animals.get(currPos);
                if (currPosAnimalList.size() == 1) {
                    animals.remove(currPos);
                } else {
                    currPosAnimalList.remove(currAnimal);
                }

            }
        }
    }

    public Animal findBestAnimal(AnimalList animalList) {
        return animalList.findBestAnimal();
    }

    public void plantConsumptionOnFieldIfPossible(Vector2d position) {
        AnimalList animalsOnField = animals.get(position);
        if (animalsOnField != null && plants.get(position) != null) {
            Animal animal = animalsOnField.get(0);
            if (animalsOnField.size() > 1) {
                animal = findBestAnimal(animalsOnField);
            }
            removePlant(position);
            animal.eatPlant();
        }
    }

    public void massivePlantConsumption() {
        List<Vector2d> fieldsToIterate = new ArrayList<>();
        if (animals.size() < plants.size()) {
            for (Map.Entry<Vector2d, AnimalList> entry : animals.entrySet()) {
                fieldsToIterate.add(entry.getKey());
            }
        } else {
            for (Map.Entry<Vector2d, Plant> entry : plants.entrySet()) {
                fieldsToIterate.add(entry.getKey());
            }
        }

        // for every "field to iterate", call method plantConsumptionOnFieldIfPossible
        for (Vector2d fieldToIterate : fieldsToIterate) {
            plantConsumptionOnFieldIfPossible(fieldToIterate);
        }
    }

    public void reproduceOnFieldIfPossible(Vector2d position, int currDay) {

        // check if reproduction on the chosen field is possible (with the number of animals there)
        if (animals.get(position) == null || animals.get(position).size() < 2) {
            return;
        }

        // determine which pair of animals will reproduce
        AnimalList animalList = animals.get(position);
        Animal bestAnimal1 = findBestAnimal(animalList);
        animalList.remove(bestAnimal1);
        Animal bestAnimal2 = findBestAnimal(animalList);
        animalList.add(bestAnimal1);

        // check if the chosen pair of animals has enough energy to reproduce
        // otherwise do not reproduce
        if (bestAnimal1.getEnergy() < settings.getReproductionEnergyThreshold() || bestAnimal2.getEnergy() < settings.getReproductionEnergyThreshold()) {
            return;
        }

        // determine how many genes of the first (stronger) animal will be inherited to the child
        // other genes will be inherited from the second (weaker) animal
        int energySum = bestAnimal1.getEnergy() + bestAnimal2.getEnergy();
        double bestAnimal1EnergyShare = (double) bestAnimal1.getEnergy() / energySum;
        int bestAnimal1InheritedGeneCount = (int) (bestAnimal1EnergyShare * settings.getGenomeLength());
        int bestAnimal2InheritedGeneCount = settings.getGenomeLength() - bestAnimal1InheritedGeneCount;

        // determine (random) if the stronger animal passes the genes on the left side or on the right side of the genotype
        Random rand = new Random();
        int randRes = rand.nextInt(2); // 0 -- left, 1 -- right

        // create the genes of the child
        int[] genes1 = bestAnimal1.getGenes();
        int[] genes2 = bestAnimal2.getGenes();

        int[] childGenes = new int[settings.getGenomeLength()];

        if (randRes == 0) {
            for (int i = 0; i < bestAnimal1InheritedGeneCount; i++) {
                childGenes[i] = genes1[i];
            }
            for (int i = bestAnimal1InheritedGeneCount; i < settings.getGenomeLength(); i++) {
                childGenes[i] = genes2[i];
            }
        } else {
            for (int i = 0; i < bestAnimal2InheritedGeneCount; i++) {
                childGenes[i] = genes2[i];
            }
            for (int i = bestAnimal2InheritedGeneCount; i < settings.getGenomeLength(); i++) {
                childGenes[i] = genes1[i];
            }
        }

        // create the child (new Animal) of the animals that have reproduced
        Animal child = new Animal(position, settings, currDay);
        child.setParent1(bestAnimal1);
        child.setParent2(bestAnimal2);
        child.setGenes(childGenes);

        // transfer energy (settings.energyUsedByParents) from parents to children
        bestAnimal1.changeEnergy((-1) * settings.getEnergyUsedByParents());
        bestAnimal2.changeEnergy((-1) * settings.getEnergyUsedByParents());
        child.setEnergy(2 * settings.getEnergyUsedByParents());

        // stats change
        bestAnimal1.setChildrenCount(bestAnimal1.getChildrenCount() + 1);
        bestAnimal2.setChildrenCount(bestAnimal2.getChildrenCount() + 1);
        bestAnimal1.addChild(child);
        bestAnimal2.addChild(child);

        // mutations
        if (settings.getMutationType() == 2) {
            mutateByReplacementOrSwap(child);
        } else {
            mutateByReplacement(child);
        }

        // end reproduction
        place(child);
        mapChanged("Animals " + bestAnimal1 + " and " + bestAnimal2 + " reproduced at " + position + " creating animal " + child);
    }

    public void mutateByReplacement(Animal animal) {
        // select a random number of genes in [minMutationCount; maxMutationCount]
        Random rand = new Random();
        int upperBound = settings.getMaxMutationCount();
        int lowerBound = settings.getMinMutationCount();
        int genesToAlter = rand.nextInt(upperBound + 1 - lowerBound) + lowerBound;

        // select which genes will be altered and alter them
        int allGenesCnt = settings.getGenomeLength();
        for (int i = 0; i < genesToAlter; i++) {
            Random rand2 = new Random();
            int oldGeneIdx = rand2.nextInt(allGenesCnt);
            int newGeneVal = rand2.nextInt(8);
            int[] animalGenes = animal.getGenes();
//            System.out.println("Animal " + animal + " has changed its gene (#" + oldGeneIdx + ") " + animalGenes[oldGeneIdx] + " -> " + newGeneVal);
            animalGenes[oldGeneIdx] = newGeneVal;
            animal.setGenes(animalGenes);
        }
    }

    public void mutateByReplacementOrSwap(Animal animal) {
        // select a random number of genes in [minMutationCount; maxMutationCount]
        Random rand = new Random();
        int upperBound = settings.getMaxMutationCount();
        int lowerBound = settings.getMinMutationCount();
        int genesToAlter = rand.nextInt(upperBound + 1 - lowerBound) + lowerBound;

        // select which genes will be altered and alter them
        int allGenesCnt = settings.getGenomeLength();
        for (int i = 0; i < genesToAlter; i++) {
            // select random gene
            Random rand2 = new Random();
            int oldGeneIdx = rand2.nextInt(allGenesCnt);

            // select which mutation type will take place
            // 0 -> replacement
            // 1 -> swap
            Random rand3 = new Random();
            int singleMutationType = rand3.nextInt(2);
            int[] animalGenes = animal.getGenes();

            if (singleMutationType == 1) {
                int geneToReplaceWithIdx = rand2.nextInt(allGenesCnt);
                int geneVal1 = animalGenes[oldGeneIdx];
                int geneVal2 = animalGenes[geneToReplaceWithIdx];
                animalGenes[geneToReplaceWithIdx] = geneVal1;
                animalGenes[oldGeneIdx] = geneVal2;
//                System.out.println("Animal " + animal + " has swapped its gene (#" + oldGeneIdx + ") <-> (#" + geneToReplaceWithIdx + ")" );
            } else {
                int newGeneVal = rand2.nextInt(8);
//                System.out.println("Animal " + animal + " has changed its gene (#" + oldGeneIdx + ") " + animalGenes[oldGeneIdx] + " -> " + newGeneVal);
                animalGenes[oldGeneIdx] = newGeneVal;
                animal.setGenes(animalGenes);
            }
        }
    }

    public void reproduceOnEveryPossibleField(int currDay) {
        List<Vector2d> fieldsToIterate = new ArrayList<>();
        for (Map.Entry<Vector2d, AnimalList> entry : animals.entrySet()) {
            fieldsToIterate.add(entry.getKey());
        }
        for (Vector2d fieldToIterate : fieldsToIterate) {
            reproduceOnFieldIfPossible(fieldToIterate, currDay);
        }
    }

    public Vector2d randomField() {
        Random rand1 = new Random();
        int randX = rand1.nextInt(width);
        int randY = rand1.nextInt(height);
        return new Vector2d(randX, randY);
    }

    public Vector2d randomFieldEquator() {
        Random rand1 = new Random();
        int randX = rand1.nextInt(width);
        int randY = rand1.nextInt(maxEquatorHeight - minEquatorHeight) + minEquatorHeight;
        return new Vector2d(randX, randY);
    }

    public Vector2d randomFieldNonEquator() {
        Random rand1 = new Random();
        int aboveEquator = rand1.nextInt(2);
        int randX = rand1.nextInt(width);
        int randY;

        if (aboveEquator == 1) {
            randY = rand1.nextInt(height - maxEquatorHeight) + maxEquatorHeight;
        } else {
            randY = rand1.nextInt(minEquatorHeight);
        }
        return new Vector2d(randX, randY);
    }


    public Map<Vector2d, AnimalList> getAnimals() {
        return animals;
    }

    public int getActivatedGeneIdx(Animal animal, int dayNo) {
        return (animal.getStartGeneId() + dayNo) % settings.getGenomeLength();
    }

    public int getAnimalCount() {
        int res = 0;

        for (Map.Entry<Vector2d, AnimalList> entry : animals.entrySet()) {
            AnimalList currList = entry.getValue();
            res += currList.size();
        }

        return res;
    }

    public int getAnimalFieldCount() {
        int res = 0;

        for (Map.Entry<Vector2d, AnimalList> entry : animals.entrySet()) {
            AnimalList currList = entry.getValue();
            res++;
        }

        return res;
    }

    public int getPlantCount() {
        return plants.size();
    }

    public int getEmptyFieldCount() {
        return ((width) * (height)) - getAnimalFieldCount() - getPlantCount();
    }

    public double getAvgEnergy() {
        List<Animal> currAnimalList = createCurrAnimalList();
        if (currAnimalList.isEmpty())
            return -1;
        int energySum = 0;
        for (Animal animal : currAnimalList) {
            energySum += animal.getEnergy();
        }
        return (double) energySum / (double) currAnimalList.size();
    }

    public double getAvgLifespanOfDeadAnimals() {
        List<Animal> deadAnimalList = deadAnimals;
        if (deadAnimalList.isEmpty())
            return -1;
        int daysLivedSum = 0;
        for (Animal animal : deadAnimalList) {
            daysLivedSum += animal.getDaysLived();
        }
        return (double) daysLivedSum / (double) deadAnimalList.size();
    }

    public double getAvgChildrenCount() {
        List<Animal> currAnimalList = createCurrAnimalList();
        if (currAnimalList.isEmpty())
            return -1;
        int childrenSum = 0;
        for (Animal animal : currAnimalList) {
            childrenSum += animal.getChildrenCount();
        }
        return (double) childrenSum / (double) currAnimalList.size();
    }

    public void increaseDayCountOfAllAnimals() {
        List<Animal> currAnimalList = createCurrAnimalList();
        for (Animal animal : currAnimalList) {
            animal.addOneDay();
        }
    }

    public int findMostFrequentGene() {
        List<Animal> currAnimalList = createCurrAnimalList();
        int[] geneOccurrences = new int[8];
        for (Animal animal : currAnimalList) {
            int[] currGenes = animal.getGenes();
            for (int gene : currGenes) {
                geneOccurrences[gene]++;
            }
        }
        int mostFrequentGene = -1;
        int mostFrequentGeneCount = -1;
        for (int i = 0; i < 8; i++) {
            if (geneOccurrences[i] > mostFrequentGeneCount) {
                mostFrequentGene = i;
                mostFrequentGeneCount = geneOccurrences[i];
            }
        }
        return mostFrequentGene;
    }

    public void removePlant(Vector2d position) {
        plants.remove(position);
        if (minEquatorHeight <= position.getY() && position.getY() < maxEquatorHeight) {
            freeEquatorFields++;
            takenEquatorFields--;
        } else {
            freeNonEquatorFields++;
            takenNonEquatorFields--;
        }
    }

    public int getFreeEquatorFields() {
        return freeEquatorFields;
    }

    public int getFreeNonEquatorFields() {
        return freeNonEquatorFields;
    }

    public int getTakenEquatorFields() {
        return takenEquatorFields;
    }

    public int getTakenNonEquatorFields() {
        return takenNonEquatorFields;
    }

    public boolean isOccupiedByWater(Vector2d position) {
        return false;
    }

    public void addGenomeToStats(Genome genome) {
        if (genomeCount.containsKey(genome)) {
            genomeCount.put(genome, genomeCount.get(genome) + 1);
        } else {
            genomeCount.put(genome, 1);
        }
    }

    public void removeGenomeFromStats(Genome genome) {
        if (!genomeCount.containsKey(genome)) {
            return;
        } else if (genomeCount.get(genome) > 1) {
            genomeCount.put(genome, genomeCount.get(genome) - 1);
        } else {
            genomeCount.remove(genome);
        }
    }

    public Map<Genome, Integer> getGenomeCount() {
        return genomeCount;
    }

    public Map<Vector2d, Plant> getPlants() {
        return plants;
    }

    public int getMinEquatorHeight() {
        return minEquatorHeight;
    }

    public int getMaxEquatorHeight() {
        return maxEquatorHeight;
    }
}
