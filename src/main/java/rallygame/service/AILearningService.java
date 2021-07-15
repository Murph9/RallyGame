package rallygame.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import io.jenetics.Phenotype;
import io.jenetics.engine.Engine;
import io.jenetics.engine.Evaluators;
import io.jenetics.engine.EvolutionResult;
import rallygame.car.ai.RaceAI;
import rallygame.car.ray.RayCarControl;
import rallygame.drive.DriveAILearn;
import rallygame.helper.Log;
import io.jenetics.DoubleChromosome;
import io.jenetics.Genotype;

@SuppressWarnings({"rawtypes", "unchecked"})
public class AILearningService {
    
    public static final Genotype ENCODING = Genotype.of(
        DoubleChromosome.of(0, 25, 1)
    );

    private final DriveAILearn drive;
    private final Executor e;

    public AILearningService(DriveAILearn drive, Executor e) {
        this.drive = drive;
        this.e = e;
    }

    public void run() {
        System.out.println("Loading AI learning environment");

        // Create the execution environment.
        final Engine engine = new Engine.Builder(Evaluators.completable(this::asyncFitness), AILearningService.ENCODING)
            .executor(e)
            .build();

        // Start the execution (evolution) and collect the result.
        final Phenotype result = (Phenotype)engine.stream()
            .limit(3)
            .collect(EvolutionResult.toBestPhenotype());

        System.out.println("Best found result: " + result);
    }

    
    private CompletableFuture<Double> asyncFitness(final Genotype gt) {
        return CompletableFuture.supplyAsync(new Supplier<Double>() {
            @Override
            public Double get() {
                var value = ((DoubleChromosome)gt.get(0)).floatValue();

                // create ai with args from genotype
                final RayCarControl car = drive.addCar((c) -> {
                    return new RaceAI(c, drive, value);
                });

                // wait for result in the game
                float result = 0;
                while (result == 0) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    result = drive.getScore(car);
                }
                Log.p(car, "found result: ", result, " double input as", value);

                // return
                return Double.valueOf(result);
            }
        });
    }
}
