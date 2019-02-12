DROP VIEW IF EXISTS vetd.prompts_by_req;
--;;
CREATE VIEW vetd.prompts_by_req AS SELECT "rp"."id" AS "rpid", "rp"."req_id", "p"."id", "p"."idstr", "p"."created", "p"."updated", "p"."deleted", "p"."prompt", "p"."descr" FROM "req_prompt" "rp" INNER JOIN "prompts" "p" ON "p"."id" = "rp"."prompt_id";
--;;
ALTER VIEW vetd.prompts_by_req OWNER TO vetd
--;;
GRANT SELECT ON vetd.prompts_by_req TO hasura;