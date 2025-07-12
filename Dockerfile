FROM eclipse-temurin:23-jdk

WORKDIR /app
COPY out/production/CP_Project ./CP_Project
COPY lib/ ./lib

ENTRYPOINT ["java", "-cp", "CP_Project:lib/*"]

# other main files, N_threads.Main_NThreads, single_thread.Main_single
CMD ["producer_consumer_model.Main_Prod_Cons"]
