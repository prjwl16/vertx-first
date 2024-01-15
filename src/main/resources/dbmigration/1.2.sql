-- apply changes
create table friendships (
  id                            bigint generated by default as identity not null,
  user_id                       bigint,
  friend_id                     bigint,
  created_at                    timestamptz not null,
  updated_at                    timestamptz not null,
  is_deleted                    boolean default false not null,
  friendship_id                 varchar(255),
  status                        varchar(8) default 'PENDING',
  constraint ck_friendships_status check ( status in ('PENDING','ACCEPTED','REJECTED','BLOCKED')),
  constraint uq_friendships_friendship_id unique (friendship_id),
  constraint pk_friendships primary key (id)
);
